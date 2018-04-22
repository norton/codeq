(ns datomic.codeq.analyzers.chart-yaml
  (:require [datomic.api :as d]
            [datomic.codeq.util :refer [index->id-fn tempid?]]
            [datomic.codeq.analyzer :as az]
            [yaml.core :as yaml]))

(defn analyze-1
  "returns [tx-data ctx]"
  [db f x loc seg ret {:keys [sha1->id codename->id added ns] :as ctx}]
  (if loc
    (let [sha1 (-> seg az/ws-minify az/sha1)
          codeid (sha1->id sha1)
          newcodeid (and (tempid? codeid) (not (added codeid)))
          ret (cond-> ret newcodeid (conj {:db/id codeid :code/sha1 sha1 :code/text seg}))
          added (cond-> added newcodeid (conj codeid))

          codeqid (or (ffirst (d/q '[:find ?e :in $ ?f ?loc
                                     :where [?e :codeq/file ?f] [?e :codeq/loc ?loc]]
                                   db f loc))
                      (d/tempid :db.part/user))

          apiVersion (:apiVersion x)
          name (:name x)
          version (:version x)
          kubeVersion (:kubeVersion x)
          description (:description x)
          keywords (:keywords x)
          home (:home x)
          sources (:sources x)
          maintainers (:maintainers x)
          engine (:engine x)
          icon (:icon x)
          appVersion (:appVersion x)
          deprecated (:deprecated x)
          tillerVersion (:tillerVersion x)

          ret (cond-> ret
                (tempid? codeqid)
                (conj {:db/id codeqid
                       :codeq/file f
                       :codeq/loc loc
                       :codeq/code codeid})

                (some? apiVersion)
                (conj [:db/add codeqid :chart-yaml/apiVersion (str apiVersion)])

                (some? name)
                (conj [:db/add codeqid :chart-yaml/name name])

                (some? version)
                (conj [:db/add codeqid :chart-yaml/version (str version)])

                (some? kubeVersion)
                (conj [:db/add codeqid :chart-yaml/kubeVersion (str kubeVersion)])

                (some? description)
                (conj [:db/add codeqid :chart-yaml/description description])

                (some? keywords)
                (concat (map #(vector :db/add codeqid :chart-yaml/keywords %) keywords))

                (some? home)
                (conj [:db/add codeqid :chart-yaml/home (new java.net.URI home)])

                (some? sources)
                (concat (map #(vector :db/add codeqid :chart-yaml/sources (new java.net.URI %)) sources))

                (some? maintainers)
                (concat (mapcat #(let [mid (d/tempid :db.part/user)
                                       mname (:name %)
                                       memail (:email %)
                                       murl (:url %)]
                                   (cond-> [[:db/add codeqid :chart-yaml/maintainers mid]
                                            [:db/add mid :chart-maintainer/name mname]]
                                     (some? memail)
                                     (conj [:db/add mid :chart-maintainer/email memail])
                                     (some? murl)
                                     (conj [:db/add mid :chart-maintainer/url (new java.net.URI murl)])))
                                maintainers))

                (some? engine)
                (conj [:db/add codeqid :chart-yaml/engine engine])

                (some? icon)
                (conj [:db/add codeqid :chart-yaml/icon (new java.net.URI icon)])

                (some? appVersion)
                (conj [:db/add codeqid :chart-yaml/appVersion (str appVersion)])

                (some? deprecated)
                (conj [:db/add codeqid :chart-yaml/deprecated deprecated])

                (some? tillerVersion)
                (conj [:db/add codeqid :chart-yaml/tillerVersion (str tillerVersion)]))]

      [ret (assoc ctx :added added)])
    [ret ctx]))

(defn analyze
   [db f src]
  (let [ctx {:sha1->id (index->id-fn db :code/sha1)
             :codename->id (index->id-fn db :code/name)
             :added #{}}
        x (yaml/parse-string src)
        loc "0 0 0 0" ;; TODO
        seg src
        ret []
        [ret ctx] (analyze-1 db f x loc seg ret ctx)]
    ret))

(defn schemas []
  {1 [{:db/ident :chart-yaml/apiVersion
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "The version of the api that this contains (required)"}
      {:db/ident :chart-yaml/name
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "The name of the chart (required)"}
      {:db/ident :chart-yaml/version
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "A SemVer 2 version (required)"}
      {:db/ident :chart-yaml/kubeVersion
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "A SemVer range of compatible Kubernetes versions (optional)"}
      {:db/ident :chart-yaml/description
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "A single-sentence description of this project (optional)"}
      {:db/ident :chart-yaml/keywords
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/many
       :db/doc "A list of keywords about this project (optional)"}
      {:db/ident :chart-yaml/home
       :db/valueType :db.type/uri
       :db/cardinality :db.cardinality/one
       :db/doc "The URL of this project's home page (optional)"}
      {:db/ident :chart-yaml/sources
       :db/valueType :db.type/uri
       :db/cardinality :db.cardinality/many
       :db/doc "A list of URLs to source code for this project (optional)"}
      {:db/ident :chart-yaml/maintainers
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/many
       :db/doc "# (optional)"}
      {:db/ident :chart-maintainer/name
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "The maintainer's name (required for each maintainer)"}
      {:db/ident :chart-maintainer/email
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "The maintainer's email (optional for each maintainer)"}
      {:db/ident :chart-maintainer/url
       :db/valueType :db.type/uri
       :db/cardinality :db.cardinality/one
       :db/doc "A URL for the maintainer (optional for each maintainer)"}
      {:db/ident :chart-yaml/engine
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "The name of the template engine (optional, defaults to gotpl)"}
      {:db/ident :chart-yaml/icon
       :db/valueType :db.type/uri
       :db/cardinality :db.cardinality/one
       :db/doc "A URL to an SVG or PNG image to be used as an icon (optional)."}
      {:db/ident :chart-yaml/appVersion
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "The version of the app that this contains (optional). This needn't be SemVer."}
      {:db/ident :chart-yaml/deprecated
       :db/valueType :db.type/boolean
       :db/cardinality :db.cardinality/one
       :db/doc "Whether this chart is deprecated (optional, boolean)"}
      {:db/ident :chart-yaml/tillerVersion
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "The version of Tiller that this chart requires. This should be expressed as a SemVer range: \">2.0.0\" (optional)"}]})

(deftype ChartYamlAnalyzer []
  az/Analyzer
  (keyname [a] :chart-yaml)
  (revision [a] 1)
  (extensions [a] ["Chart.yaml"])
  (schemas [a] (schemas))
  (analyze [a db f src] (analyze db f src)))

(defn impl [] (ChartYamlAnalyzer.))
