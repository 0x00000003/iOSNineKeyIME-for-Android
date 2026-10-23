// SPDX-License-Identifier: GPL-3.0-or-later
// Real C API integration; all input is fixed synthetic fixture data.
#include <rime_api.h>
#include <rime/context.h>
#include <rime/service.h>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <map>
#include <string>
#include <vector>

extern void rime_require_module_lua();
extern void rime_require_module_octagram();
extern void rime_require_module_predict();

namespace {
RimeApi* api;
using Tree = std::map<std::string, std::pair<std::filesystem::file_time_type, std::string>>;
Tree snapshot(const std::filesystem::path& root) {
  Tree result;
  result["."] = {std::filesystem::last_write_time(root), {}};
  for (const auto& entry : std::filesystem::recursive_directory_iterator(root)) {
    std::string bytes;
    if (entry.is_regular_file()) {
      std::ifstream input(entry.path(), std::ios::binary);
      bytes.assign(std::istreambuf_iterator<char>(input), std::istreambuf_iterator<char>());
    }
    result[std::filesystem::relative(entry.path(), root).string()] =
        {std::filesystem::last_write_time(entry.path()), std::move(bytes)};
  }
  return result;
}
void type(RimeSessionId session, const std::string& digits) {
  api->clear_composition(session);
  for (char digit : digits) api->process_key(session, digit, 0);
}
std::vector<std::string> menu(RimeSessionId session, int limit = 100) {
  std::vector<std::string> values;
  RimeCandidateListIterator iterator{};
  if (api->candidate_list_begin(session, &iterator)) {
    while (static_cast<int>(values.size()) < limit &&
           api->candidate_list_next(&iterator))
      values.emplace_back(iterator.candidate.text);
    api->candidate_list_end(&iterator);
  }
  return values;
}
int rank(RimeSessionId session, const std::string& text) {
  auto values = menu(session);
  for (size_t i = 0; i < values.size(); ++i)
    if (values[i] == text) return static_cast<int>(i) + 1;
  return -1;
}
std::string takeCommit(RimeSessionId session) {
  RIME_STRUCT(RimeCommit, commit);
  if (!api->get_commit(session, &commit)) return {};
  std::string text(commit.text);
  api->free_commit(&commit);
  return text;
}
int length(RimeSessionId session) {
  RIME_STRUCT(RimeContext, context);
  if (!api->get_context(session, &context)) return -1;
  int result = context.composition.length;
  api->free_context(&context);
  return result;
}
int historySize(RimeSessionId session) {
  auto handle = rime::Service::instance().GetSession(session);
  return handle ? static_cast<int>(handle->context()->commit_history().size()) : -1;
}
void start(const char* shared, const char* user) {
  RIME_STRUCT(RimeTraits, traits);
  traits.shared_data_dir = shared;
  traits.user_data_dir = user;
  traits.app_name = "ime.host.compatibility";
  traits.min_log_level = 3;
  traits.log_dir = "";
  api->initialize(&traits);
  api->start_maintenance(true);
  api->join_maintenance_thread();
}
}  // namespace

int main(int argc, char** argv) {
  if ((argc != 3 && argc != 4) || std::filesystem::exists(argv[2])) return 64;
  bool noLearning = argc == 4 && std::string(argv[3]) == "--no-learning";
  bool freshRanks = argc == 4 && std::string(argv[3]) == "--fresh-context-ranks";
  if (argc == 4 && !noLearning && !freshRanks) return 64;
  std::filesystem::create_directories(argv[2]);
  rime_require_module_lua();
  rime_require_module_octagram();
  rime_require_module_predict();
  api = rime_get_api();
  RIME_STRUCT(RimeTraits, traits);
  traits.app_name = "ime.host.compatibility";
  api->setup(&traits);
  start(argv[1], argv[2]);
  auto session = api->create_session();
  bool schema = api->select_schema(session, "wanxiang_t9");
  const auto initialTree = noLearning ? snapshot(argv[2]) : Tree{};
  int multi = 0;
  for (const auto& fixture : std::vector<std::pair<std::string, std::string>>{
           {"64426", "你好"}, {"94664486", "中国"}, {"744543", "世界"}}) {
    type(session, fixture.first);
    int found = rank(session, fixture.second);
    if (found > 0 && api->select_candidate(session, found - 1) &&
        takeCommit(session) == fixture.second && takeCommit(session).empty() &&
        length(session) == 0) ++multi;
  }

  // A shorter candidate confirms one segment, leaving the rest composed.
  type(session, "64426744543");
  auto values = menu(session, 500);
  int prefixIndex = -1;
  for (size_t i = 0; i < values.size(); ++i)
    if (values[i] == "你好") { prefixIndex = static_cast<int>(i); break; }
  bool partial = prefixIndex >= 0 && api->select_candidate(session, prefixIndex);
  partial = partial && length(session) > 0 && !menu(session).empty();
  auto partialCommit = takeCommit(session);
  bool drained = partial && api->commit_composition(session);
  auto finalCommit = takeCommit(session);
  drained = drained && !finalCommit.empty() && length(session) == 0 &&
            takeCommit(session).empty();
  // Actual partial commits may remain buffered by express_editor until finalization.
  bool exactDrain = partialCommit + finalCommit == "你好世界";

  if (freshRanks) {
    api->destroy_session(session);
    session = api->create_session();
    api->select_schema(session, "wanxiang_t9");
  }
  int baselineHistory = historySize(session);
  type(session, "64426");
  values = menu(session);
  std::string learned = values.size() > 1 ? values[1] : "";
  int before = learned.empty() ? -1 : rank(session, learned);
  bool training = !learned.empty();
  for (int repeat = 0; repeat < 20 && training; ++repeat) {
    type(session, "64426");
    int current = rank(session, learned);
    training = current > 0 && api->select_candidate(session, current - 1) &&
               takeCommit(session) == learned && takeCommit(session).empty();
    // An unhandled key ends the pending learning transaction.
    api->process_key(session, 0xffe1, 0);
  }
  type(session, "64426");
  int after = rank(session, learned);
  int concurrentFreshRank = -1;
  int concurrentFreshHistory = -1;
  bool originalSessionAlive = false;
  if (freshRanks) {
    // Keep A and its DB handle alive while measuring B, so closing/flushing A is not the cause.
    auto comparison = api->create_session();
    api->select_schema(comparison, "wanxiang_t9");
    concurrentFreshHistory = historySize(comparison);
    type(comparison, "64426");
    concurrentFreshRank = rank(comparison, learned);
    originalSessionAlive = api->find_session(session);
    api->clear_composition(comparison);
    api->destroy_session(comparison);
  }
  int historyBeforeClear = historySize(session);
  api->clear_composition(session);
  int historyAfterClear = historySize(session);
  api->destroy_session(session);
  session = api->create_session();
  api->select_schema(session, "wanxiang_t9");
  int historyFresh = historySize(session);
  type(session, "64426");
  int freshSessionRank = rank(session, learned);
  int measuredAfter = freshRanks ? freshSessionRank : after;
  api->clear_composition(session);
  api->destroy_session(session);
  api->finalize();
  bool unchanged = noLearning && initialTree == snapshot(argv[2]);

  start(argv[1], argv[2]);
  session = api->create_session();
  api->select_schema(session, "wanxiang_t9");
  type(session, "64426");
  int restarted = rank(session, learned);
  bool database = false;
  for (auto const& entry : std::filesystem::directory_iterator(argv[2]))
    if (entry.path().filename().string().find(".userdb") != std::string::npos)
      database = true;
  bool learning = training && before > 1 && measuredAfter > 0 && restarted == measuredAfter &&
      (noLearning ? (!database && before == measuredAfter && unchanged) : (database && measuredAfter < before)) &&
      (!freshRanks || (baselineHistory == 0 && historyFresh == 0 && concurrentFreshHistory == 0 && concurrentFreshRank == measuredAfter && originalSessionAlive));
  api->clear_composition(session);
  api->destroy_session(session);
  api->finalize();
  bool pass = schema && multi == 3 && partial && drained && exactDrain && learning;
  std::cout << "{\"scope\":\"host C API schema integration; not Android or R09 ten-word acceptance\","
            << "\"schema\":" << schema << ",\"multi_syllable_fixtures_passed\":" << multi
            << ",\"partial_selection_preserves_composition\":" << partial
            << ",\"partial_drain_single_exact_commit\":" << (drained && exactDrain)
            << ",\"learning_repetitions\":20,\"learning_before_rank\":" << before
            << ",\"learning_after_rank\":" << after << ",\"restart_rank\":" << restarted
            << ",\"fresh_context_rank_control\":" << freshRanks
            << ",\"baseline_history_count\":" << baselineHistory
            << ",\"controlled_after_rank\":" << measuredAfter
            << ",\"concurrent_fresh_session_rank\":" << concurrentFreshRank
            << ",\"concurrent_fresh_session_history_count\":" << concurrentFreshHistory
            << ",\"original_training_session_alive\":" << originalSessionAlive
            << ",\"history_before_clear_count\":" << historyBeforeClear
            << ",\"history_after_clear_count\":" << historyAfterClear
            << ",\"fresh_session_history_count\":" << historyFresh
            << ",\"fresh_session_rank\":" << freshSessionRank
            << ",\"no_learning_configuration\":" << noLearning
            << ",\"runtime_profile_bytes_and_timestamps_unchanged\":" << unchanged
            << ",\"user_database_created\":" << database << ",\"learning_pass\":" << learning
            << ",\"pass\":" << pass << "}\n";
  return pass ? 0 : 1;
}
