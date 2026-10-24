// SPDX-License-Identifier: GPL-3.0-or-later
// Fixed synthetic fixtures only. Never print input, comments or candidate text.
#include <rime_api.h>
#include <rime/context.h>
#include <rime/candidate.h>
#include <rime/service.h>
#include <filesystem>
#include <iostream>
#include <string>
#include <tuple>
#include <vector>

extern void rime_require_module_lua();
extern void rime_require_module_octagram();
extern void rime_require_module_predict();
namespace {
RimeApi* api;
struct Candidate { std::string text, comment; };
std::vector<Candidate> menu(RimeSessionId s) {
  std::vector<Candidate> result;
  RimeCandidateListIterator i{};
  if (api->candidate_list_begin(s, &i)) {
    while (result.size() < 100 && api->candidate_list_next(&i))
      result.push_back({i.candidate.text, i.candidate.comment ? i.candidate.comment : ""});
    api->candidate_list_end(&i);
  }
  return result;
}
int index(RimeSessionId s, const std::string& expected) {
  auto rows = menu(s);
  for (size_t i = 0; i < rows.size(); ++i) if (rows[i].text == expected) return i;
  return -1;
}
std::string input(RimeSessionId s) { auto p = api->get_input(s); return p ? p : ""; }
std::string commit(RimeSessionId s) {
  RIME_STRUCT(RimeCommit, c);
  if (!api->get_commit(s, &c)) return {};
  std::string result(c.text); api->free_commit(&c); return result;
}
bool type(RimeSessionId s, const std::string& value) {
  api->clear_composition(s);
  bool result = true;
  for (char c : value) result &= api->process_key(s, c, 0);
  return result;
}
std::string folded(std::string value) {
  for (auto [from, to] : std::vector<std::pair<std::string, std::string>>{
      {"ā","a"},{"á","a"},{"ǎ","a"},{"à","a"},{"ē","e"},{"é","e"},{"ě","e"},{"è","e"},
      {"ī","i"},{"í","i"},{"ǐ","i"},{"ì","i"},{"ō","o"},{"ó","o"},{"ǒ","o"},{"ò","o"},
      {"ū","u"},{"ú","u"},{"ǔ","u"},{"ù","u"},{"ü","v"},{"ǖ","v"},{"ǘ","v"},{"ǚ","v"},{"ǜ","v"}}) {
    size_t p = 0;
    while ((p = value.find(from, p)) != std::string::npos) { value.replace(p, from.size(), to); p += to.size(); }
  }
  return value;
}
using Selected = std::vector<std::tuple<size_t, size_t, std::string>>;
Selected selected(RimeSessionId s) {
  Selected result;
  auto session = rime::Service::instance().GetSession(s);
  for (const auto& seg : session->context()->composition()) {
    if (seg.status < rime::Segment::kSelected) continue;
    auto c = seg.GetSelectedCandidate();
    if (c) result.emplace_back(seg.start, seg.end, c->text());
  }
  return result;
}
}
int main(int argc, char** argv) {
  if (argc != 3) return 64;
  try {
    if (!std::filesystem::create_directory(argv[2])) return 65;
    rime_require_module_lua(); rime_require_module_octagram(); rime_require_module_predict();
    api = rime_get_api();
    RIME_STRUCT(RimeTraits, traits);
    traits.shared_data_dir = argv[1]; traits.user_data_dir = argv[2];
    traits.app_name = "ime.pinyin.compatibility"; traits.log_dir = ""; traits.min_log_level = 3;
    api->setup(&traits); api->initialize(&traits);
    api->start_maintenance(true); api->join_maintenance_thread();
    auto s = api->create_session();
    bool available = RIME_API_AVAILABLE(api, set_input);
    bool schema = api->select_schema(s, "wanxiang_t9_nolearn");
    bool original = type(s, "64426") && index(s, "你好") >= 0 && commit(s).empty();
    bool comments = false; int commentCount = 0;
    for (const auto& row : menu(s)) {
      commentCount += !row.comment.empty();
      auto text = folded(row.comment);
      if (row.text == "你好") comments = text.find("ni") != std::string::npos && text.find("hao") != std::string::npos;
    }
    bool mixed = available && api->set_input(s, "ni'426") && input(s) == "ni'426" && index(s, "你好") >= 0 && commit(s).empty();
    bool explicitChoice = available && api->set_input(s, "ni'hao") && input(s) == "ni'hao" && commit(s).empty();
    int hello = index(s, "你好");
    explicitChoice &= hello >= 0 && api->select_candidate(s, hello) && commit(s) == "你好" && commit(s).empty();
    type(s, "64426");
    bool alternative = available && api->set_input(s, "mi'426") && input(s) == "mi'426" && !menu(s).empty() && index(s, "你好") < 0 && commit(s).empty();
    type(s, "64426744543");
    int prefix = index(s, "你好");
    bool partial = prefix >= 0 && api->select_candidate(s, prefix);
    std::string first = commit(s);
    auto previous = selected(s);
    auto ctx = rime::Service::instance().GetSession(s)->context();
    auto start = ctx->composition().GetConfirmedPosition();
    auto raw = input(s);
    bool tail = start <= raw.size() && raw.substr(start) == "744543";
    std::string replacement = raw.substr(0, start) + (start ? "'" : "") + "shi'543";
    bool retained = partial && tail && available && api->set_input(s, replacement.c_str()) && input(s) == replacement && commit(s).empty() && selected(s) == previous;
    int world = index(s, "世界");
    bool partialExact = retained && world >= 0 && api->select_candidate(s, world) && first + commit(s) == "你好世界" && commit(s).empty();
    bool pass = available && schema && original && comments && mixed && explicitChoice && alternative && retained && partialExact;
    std::cout << std::boolalpha << "{\"api_available\":" << available << ",\"schema_selected\":" << schema
              << ",\"original_fixture\":" << original << ",\"phonetic_comments_usable\":" << comments
              << ",\"nonempty_comments\":" << commentCount << ",\"mixed_rewrite_no_commit\":" << mixed
              << ",\"explicit_syllables_single_commit\":" << explicitChoice << ",\"alternative_excludes_original\":" << alternative
              << ",\"partial_tail_range_matches\":" << tail << ",\"selected_segments_retained\":" << retained
              << ",\"partial_commit_exact_once\":" << partialExact << ",\"pass\":" << pass << "}\n";
    api->destroy_session(s); api->finalize(); return pass ? 0 : 1;
  } catch (...) { std::cerr << "E_PINYIN_COMPATIBILITY_PROBE\n"; return 1; }
}
