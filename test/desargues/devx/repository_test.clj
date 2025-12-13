(ns desargues.devx.repository-test
  "Tests for the repository pattern."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [desargues.devx.repository :as repo]))

;; =============================================================================
;; Test Data
;; =============================================================================

(def test-graph
  {:segments {:intro {:id :intro :state :dirty}
              :main {:id :main :state :dirty :deps #{:intro}}}
   :edges {:intro #{:main}}
   :metadata {:title "Test Scene"}})

;; =============================================================================
;; Fixtures
;; =============================================================================

(defn with-clean-memory-repo [f]
  (repo/set-repository! (repo/memory-repository))
  (f))

(use-fixtures :each with-clean-memory-repo)

;; =============================================================================
;; Memory Repository Tests
;; =============================================================================

(deftest test-memory-repo-save-load
  (testing "Can save and load a graph"
    (repo/save! :test test-graph)
    (let [loaded (repo/load! :test)]
      (is (= test-graph loaded)))))

(deftest test-memory-repo-exists
  (testing "exists? returns correct values"
    (is (not (repo/exists? :nonexistent)))
    (repo/save! :test test-graph)
    (is (repo/exists? :test))))

(deftest test-memory-repo-list
  (testing "list-all returns all saved graphs"
    (repo/save! :project-a test-graph)
    (repo/save! :project-b test-graph)
    (let [all (set (repo/list-all))]
      (is (contains? all :project-a))
      (is (contains? all :project-b)))))

(deftest test-memory-repo-delete
  (testing "Can delete a graph"
    (repo/save! :to-delete test-graph)
    (is (repo/exists? :to-delete))
    (is (true? (repo/delete! :to-delete)))
    (is (not (repo/exists? :to-delete)))))

(deftest test-memory-repo-delete-nonexistent
  (testing "Deleting nonexistent returns false"
    (is (false? (repo/delete! :nonexistent)))))

(deftest test-memory-repo-overwrite
  (testing "Saving with same ID overwrites"
    (repo/save! :test {:version 1})
    (repo/save! :test {:version 2})
    (is (= {:version 2} (repo/load! :test)))))

;; =============================================================================
;; Protocol Tests
;; =============================================================================

(deftest test-memory-repo-satisfies-protocol
  (testing "MemoryRepository satisfies IGraphRepository"
    (let [mem-repo (repo/memory-repository)]
      (is (satisfies? repo/IGraphRepository mem-repo)))))

(deftest test-file-repo-satisfies-protocol
  (testing "FileRepository satisfies IGraphRepository"
    (let [file-repo (repo/file-repository)]
      (is (satisfies? repo/IGraphRepository file-repo)))))

;; =============================================================================
;; Registry Tests
;; =============================================================================

(deftest test-list-repositories
  (testing "Built-in repositories are registered"
    (let [repos (set (repo/list-repositories))]
      (is (contains? repos :memory))
      (is (contains? repos :file)))))

(deftest test-get-unknown-repo-throws
  (testing "Getting unknown repository throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown repository"
                          (repo/get-repository :nonexistent)))))

(deftest test-set-repository
  (testing "Can set current repository"
    (repo/set-repository! :memory)
    (is (= :memory (repo/repo-name (repo/current-repo))))))

;; =============================================================================
;; Custom Repository Tests
;; =============================================================================

(defrecord TestRepository [storage]
  repo/IGraphRepository
  (repo-name [_] :test-repo)
  (save-graph [_ id graph] (swap! storage assoc id graph) id)
  (load-graph [_ id] (get @storage id))
  (graph-exists? [_ id] (contains? @storage id))
  (list-graphs [_] (keys @storage))
  (delete-graph [_ id]
    (if (contains? @storage id)
      (do (swap! storage dissoc id) true)
      false)))

(deftest test-register-custom-repository
  (testing "Can register and use custom repository"
    (let [custom (->TestRepository (atom {}))]
      (repo/register-repository! :custom custom)
      (repo/set-repository! :custom)
      (repo/save! :my-graph test-graph)
      (is (= test-graph (repo/load! :my-graph))))))
