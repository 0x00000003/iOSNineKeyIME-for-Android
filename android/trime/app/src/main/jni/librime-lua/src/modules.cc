#include <cstdio>
#include <rime/common.h>
#include <rime/registry.h>
#include <rime_api.h>
#include <rime/service.h>
#include "lib/lua_templates.h"
#include "lua_gears.h"

void types_init(lua_State *L);

namespace {
template<typename> using void_t = void;

template<typename T, typename = void>
struct COMPAT {
  static std::string get_shared_data_dir() {
    return std::string(rime_get_api()->get_shared_data_dir());
  }

  static std::string get_user_data_dir() {
    return std::string(rime_get_api()->get_user_data_dir());
  }
};

template<typename T>
struct COMPAT<T, void_t<decltype(std::declval<T>().user_data_dir.string())>> {
  static std::string get_shared_data_dir() {
    // path::string() returns native encoding on Windows
    T &deployer = rime::Service::instance().deployer();
    return deployer.shared_data_dir.string();
  }

  static std::string get_user_data_dir() {
    T &deployer = rime::Service::instance().deployer();
    return deployer.user_data_dir.string();
  }
};
}

// Only this audited built-in module is supported. No user rime.lua or arbitrary package search.
static int offline_require(lua_State* L) {
  size_t length = 0;
  const char* name = luaL_checklstring(L, 1, &length);
  if (std::string(name, length) != "ime_probe")
    return luaL_error(L, "E_LUA_MODULE_DENIED");
  lua_getfield(L, lua_upvalueindex(2), "ime_probe");
  if (!lua_isnil(L, -1)) return 1;
  lua_pop(L, 1);
  const char* file = lua_tostring(L, lua_upvalueindex(1));
  if (luaL_loadfilex(L, file, "t") != LUA_OK || lua_pcall(L, 0, 1, 0) != LUA_OK) {
    lua_pop(L, 1);
    return luaL_error(L, "E_LUA_BUILTIN_LOAD");
  }
  lua_pushvalue(L, -1);
  lua_setfield(L, lua_upvalueindex(2), "ime_probe");
  return 1;
}

static void lua_init(lua_State *L) {
  types_init(L);
  const auto file = COMPAT<rime::Deployer>::get_shared_data_dir() + LUA_DIRSEP "lua" LUA_DIRSEP "ime_probe.lua";
  lua_pushlstring(L, file.data(), file.size());
  lua_newtable(L);
  lua_pushcclosure(L, offline_require, 2);
  lua_setglobal(L, "require");
}

static void rime_lua_initialize() {
  using namespace rime;

  LOG(INFO) << "registering components from module 'lua'.";
  Registry& r = Registry::instance();

  an<Lua> lua(new Lua);
  lua->to_state(lua_init);

  r.Register("lua_translator", new LuaComponent<LuaTranslator>(lua));
  r.Register("lua_filter", new LuaComponent<LuaFilter>(lua));
  r.Register("lua_segmentor", new LuaComponent<LuaSegmentor>(lua));
  r.Register("lua_processor", new LuaComponent<LuaProcessor>(lua));
}

static void rime_lua_finalize() {
}

RIME_REGISTER_MODULE(lua)
