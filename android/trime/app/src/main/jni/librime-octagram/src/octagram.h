#ifndef RIME_OCTAGRAM_H_
#define RIME_OCTAGRAM_H_

#include <rime/common.h>
#include <cstdint>
#include <rime/component.h>
#include <rime/resource.h>
#include <rime/gear/grammar.h>

namespace rime {

extern const ResourceType kGramDbType;
extern const string kGrammarDefaultLanguage;

class Config;
class GramDb;
struct GrammarConfig;
class OctagramComponent;

class Octagram : public Grammar {
 public:
  Octagram(Config* config, OctagramComponent* component);
  virtual ~Octagram();
  double Query(const string& context,
               const string& word,
               bool is_rear) override;

 private:
  the<GrammarConfig> config_;
  GramDb* db_ = nullptr;
  OctagramComponent* component_ = nullptr;
};

class OctagramComponent : public Grammar::Component {
 public:
  OctagramComponent();
  virtual ~OctagramComponent();

  Octagram* Create(Config* config) override;

  GramDb* GetDb(const string& language);

  // Project observability: metadata only, never load a model from an inspection call.
  bool HasLoadedDb(const string& language) const {
    auto found = db_by_language_.find(language);
    return found != db_by_language_.end() && found->second != nullptr;
  }
  void ObserveQuery() { ++query_count_; }
  uint64_t query_count() const { return query_count_; }

 private:
  map<string, the<GramDb>> db_by_language_;
  uint64_t query_count_ = 0;
};

}  // namespace rime

#endif  // RIME_OCTAGRAM_H_
