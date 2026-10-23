// SPDX-License-Identifier: GPL-3.0-or-later
// Local format/lookup study only. Does not package or redistribute the model.
#include "gram_db.h"
#include "gram_encoding.h"
#include <chrono>
#include <iostream>
#include <string>
#include <vector>

int main(int argc, char** argv) {
  if (argc != 2) return 64;
  const auto start = std::chrono::steady_clock::now();
  rime::GramDb db{rime::path(argv[1])};
  bool loaded = db.Load();
  const auto loadMs = std::chrono::duration_cast<std::chrono::milliseconds>(
      std::chrono::steady_clock::now() - start).count();
  int matched = 0;
  if (loaded) for (const auto& fixture : std::vector<std::pair<std::string, std::string>>{
      {"青花", "瓷"}, {"住在", "里屋"}, {"出了", "酒店"}, {"日常", "打字"}}) {
    rime::GramDb::Match matches[rime::GramDb::kMaxResults];
    if (db.Lookup(rime::grammar::encode(fixture.first),
                  rime::grammar::encode(fixture.second), matches) > 0) ++matched;
  }
  std::cout << "{\"scope\":\"local host model-format/lookup study; NOT Android packaging, ranking or licensing approval\","
            << "\"model_loaded\":" << loaded << ",\"load_ms\":" << loadMs
            << ",\"fixed_queries\":4,\"queries_with_matches\":" << matched
            << ",\"pass\":" << (loaded && matched == 4) << "}\n";
  return loaded && matched == 4 ? 0 : 1;
}
