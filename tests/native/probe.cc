// SPDX-License-Identifier: GPL-3.0-or-later
#include <rime_api.h>
#include <iostream>
#include <string>
#include <cstring>

extern void rime_require_module_lua();
extern void rime_require_module_octagram();
extern void rime_require_module_predict();

int main(int argc, char** argv) {
  if (argc != 3) return 64;
  rime_require_module_lua();
  rime_require_module_octagram();
  rime_require_module_predict();
  auto* api = rime_get_api();
  RIME_STRUCT(RimeTraits, traits);
  traits.shared_data_dir = argv[1];
  traits.user_data_dir = argv[2];
  traits.app_name = "ime.host.probe";
  traits.log_dir = "";
  traits.min_log_level = 3;
  api->setup(&traits);
  api->initialize(&traits);
  bool plugins = api->find_module("lua") && api->find_module("octagram") && api->find_module("predict");
  std::cout << "rime_version=" << api->get_version() << " plugins=" << plugins << '\n';
  api->start_maintenance(true);
  api->join_maintenance_thread();
  auto session = api->create_session();
  bool selected = api->select_schema(session, "wanxiang_t9");
  std::cout << "schema_selected=" << selected << '\n';
  bool handled = true;
  for (char ch : std::string("64426")) handled &= api->process_key(session, ch, 0);
  RIME_STRUCT(RimeContext, ctx);
  bool context = api->get_context(session, &ctx);
  int rank = -1;
  if (context) {
    for (int i = 0; i < ctx.menu.num_candidates; ++i)
      if (std::string(ctx.menu.candidates[i].text) == "你好") rank = i + 1;
    api->free_context(&ctx);
  }
  std::cout << "synthetic_64426_handled=" << handled << " target_rank=" << rank << '\n';
  char marker[64] = {};
  bool lua_executed = api->get_property(session, "ime_probe_lua", marker, sizeof(marker)) && std::string(marker) == "executed";
  bool selection = rank > 0 && api->select_candidate(session, rank - 1);
  RIME_STRUCT(RimeCommit, commit);
  bool single_commit = selection && api->get_commit(session, &commit);
  if (single_commit) {
    single_commit = std::string(commit.text) == "你好";
    api->free_commit(&commit);
  }
  bool no_duplicate = !api->get_commit(session, &commit);
  if (!no_duplicate) api->free_commit(&commit);
  api->clear_composition(session);
  bool backspace = api->process_key(session, '6', 0) && api->process_key(session, 0xff08, 0);
  RIME_STRUCT(RimeContext, empty);
  if (api->get_context(session, &empty)) {
    backspace &= empty.composition.length == 0;
    api->free_context(&empty);
  } else { backspace = false; }
  std::cout << "lua_executed=" << lua_executed << " single_commit=" << single_commit
            << " no_duplicate=" << no_duplicate << " delete_to_empty=" << backspace << '\n';
  api->clear_composition(session);
  api->destroy_session(session);
  api->finalize();
  return plugins && selected && handled && rank > 0 && lua_executed && single_commit && no_duplicate && backspace ? 0 : 1;
}
