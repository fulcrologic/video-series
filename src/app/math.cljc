(ns app.math
  (:refer-clojure :exclude [+ - * = < > <= >= max min])
  (:require
    #?@(:cljs [["big.js" :as Big]
               [goog.object :as obj]])
    [clojure.spec.alpha :as s]
    [ghostwheel.core :refer [>defn >defn- =>]]
    [cognitect.transit :as ct]
    [clojure.string :as str]
    [cognitect.transit :as ct])
  #?(:clj (:import (java.math RoundingMode))))

(declare * div + - < > <= >= max min)

(>defn bigdec->str [bd]
  [(s/nilable ::bigdecimal) => string?]
  (if bd
    #?(:cljs (or (some-> bd .-rep) "0")
       :clj  (str bd))
    ""))

(>defn bigdecimal?
  "Predicate for clj(s) bigdecimal"
  [v]
  [any? => boolean?]
  #?(:clj  (instance? java.math.BigDecimal v)
     :cljs (ct/bigdec? v)))

(>defn positive?
  "Predicate for clj(s) positive bigdecimal"
  [v]
  [any? => boolean?]
  (and #?(:clj  (instance? java.math.BigDecimal v)
          :cljs (ct/bigdec? v))
    (< 0 v)))

(>defn negative?
  "Predicate for clj(s) negative bigdecimal"
  [v]
  [any? => boolean?]
  (and #?(:clj  (instance? java.math.BigDecimal v)
          :cljs (ct/bigdec? v))
    (< v 0)))

(declare bigdecimal)

(s/def ::bigdecimal
  (s/with-gen bigdecimal? #(s/gen #{(bigdecimal "11.35")
                                    (bigdecimal "5.00")
                                    (bigdecimal "42.11")})))

(defn strip-zeroes [s]
  #?(:clj  s
     :cljs (-> s
             (str/replace #"^0+([1-9].*)$" "$1")
             (str/replace #"^0*([.].*)$" "0$1"))))

(>defn bigdecimal
  "Coerce to a bigdecimal from an arbitrary type."
  [s]
  [any? => ::bigdecimal]
  (if (bigdecimal? s)
    s
    (let [s (if (seq (str s)) s "0")]
      #?(:clj  (new java.math.BigDecimal (.toString s))
         :cljs (ct/bigdec (strip-zeroes (.toString s)))))))

(defn- n->big
  "Convert a number-like thing into a low-level js Big representation."
  [n]
  (cond
    (bigdecimal? n)
    #?(:clj  n
       :cljs (Big. (if (seq (bigdec->str n))
                     (bigdec->str n)
                     "0")))
    (and n (not= "" (.toString n)))
    #?(:clj  (bigdecimal (.toString n))
       :cljs (Big. (.toString n)))
    :else-if-nil-or-empty-string
    #?(:clj  (bigdecimal "0")
       :cljs (Big. "0"))))

(defn- big->bigdec
  "Convert a low-level js Big number into a bigdecimal."
  [n]
  #?(:clj  n
     :cljs (bigdecimal (.toString n))))

(defn +
  "Add the given numbers together, coercing any that are not bigdecimal."
  ([] (bigdecimal "0"))
  ([& numbers]
   #?(:clj
      (apply clojure.core/+ (map n->big numbers))
      :cljs
      (big->bigdec
        (reduce (fn [acc n]
                  (.plus acc (n->big n)))
          (Big. "0")
          numbers)))))

#?(:cljs
   (do
     (defn- big-eq [x y]
       (.eq (n->big x) (n->big y)))

     (defn- big-lt [x y]
       (.lt (n->big x) (n->big y)))

     (defn- big-lte [x y]
       (.lte (n->big x) (n->big y)))

     (defn- big-gt [x y]
       (.gt (n->big x) (n->big y)))

     (defn- big-gte [x y]
       (.gte (n->big x) (n->big y)))))

(defn compare-fn
  #?(:cljs ([big-fn]
            (fn [x y & more]
              (if (big-fn x y)
                (if (next more)
                  (recur y (first more) (next more))
                  (if (first more)
                    (big-fn y (first more))
                    true))
                false)))
     :clj  ([core-fn]
            (fn [& numbers]
              (apply core-fn (map n->big numbers))))))

(def = (compare-fn #?(:cljs big-eq :clj clojure.core/=)))
(def < (compare-fn #?(:cljs big-lt :clj clojure.core/<)))
(def <= (compare-fn #?(:cljs big-lte :clj clojure.core/<=)))
(def > (compare-fn #?(:cljs big-gt :clj clojure.core/>)))
(def >= (compare-fn #?(:cljs big-gte :clj clojure.core/>=)))

(defn -
  "Subtract the given numbers, using bigdecimal math"
  [& numbers]
  #?(:clj
     (apply clojure.core/- (map n->big numbers))
     :cljs
     (big->bigdec
       (if (= 1 (count numbers))
         (.times (n->big (first numbers)) (n->big -1))
         (reduce (fn [acc n]
                   (.minus acc (n->big n)))
           (-> numbers first n->big)
           (rest numbers))))))

(defn *
  "Multiply the given numbers, using bigdecimal math"
  ([] (bigdecimal "1"))
  ([& numbers]
   #?(:clj (apply clojure.core/* (map n->big numbers))
      :cljs
           (big->bigdec
             (reduce (fn [acc n]
                       (.times acc (n->big n)))
               (Big. "1")
               numbers)))))

(defn div
  "Divide the given two numbers, using bigdecimal math, with 20 digits
  of precision."
  [n d]
  (assert (not= 0 d))
  (let [n (n->big n)
        d (n->big d)]
    #?(:clj
       (with-precision 20
         (/ n d))
       :cljs
       (big->bigdec
         (.div n d)))))

(defn round
  "Round the given number to the given number of
  decimal digits. Returns a new bigdecimal number.

  n can be nil (returns 0), a numeric string, a regular number, or a bigdecimal."
  [n decimal-digits]
  (big->bigdec
    #?(:clj
       (.setScale (n->big n) decimal-digits RoundingMode/HALF_UP)
       :cljs
       (.toFixed (n->big n) decimal-digits))))


