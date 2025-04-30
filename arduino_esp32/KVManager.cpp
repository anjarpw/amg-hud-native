// KVManager.cpp
#include "KVManager.h"
#include <Arduino.h>

KVManager::KVManager() {}

void KVManager::set(const String& key, const String& value) {
    auto it = kvStore.find(key);
    if (it == kvStore.end() || it->second != value) {
        kvStore[key] = value;
        markDirty(key);
    }
}

void KVManager::markDirty(const String& key) {
    dirtyFlags[key] = true;
}

std::vector<KVPair> KVManager::getDirtyPairs() {
    std::vector<KVPair> dirtyPairs;
    for (auto& pair : dirtyFlags) {
        if (pair.second) {
            dirtyPairs.push_back({pair.first, kvStore[pair.first]});
            pair.second = false; // Reset dirty flag
        }
    }
    return dirtyPairs;
}