(ns play-clj.entities
  (:import [com.badlogic.gdx Gdx Graphics]
           [com.badlogic.gdx.graphics Camera]
           [com.badlogic.gdx.graphics.g2d NinePatch ParticleEffect SpriteBatch
            TextureRegion]
           [com.badlogic.gdx.graphics.g3d Environment ModelBatch ModelInstance]
           [com.badlogic.gdx.graphics.glutils ShapeRenderer]
           [com.badlogic.gdx.math Matrix4]
           [com.badlogic.gdx.scenes.scene2d Actor]))

(defprotocol Entity
  (draw-entity! [this screen batch] "Draws the entity"))

(extend-protocol Entity
  clojure.lang.PersistentArrayMap
  (draw-entity! [this screen batch])
  clojure.lang.PersistentHashMap
  (draw-entity! [this screen batch]))

(defrecord TextureEntity [object] Entity
  (draw-entity! [{:keys [^TextureRegion object x y width height
                         scale-x scale-y angle origin-x origin-y]}
                 _
                 batch]
    (let [x (float (or x 0))
          y (float (or y 0))
          width (float (or width (.getRegionWidth object)))
          height (float (or height (.getRegionHeight object)))]
      (if (or scale-x scale-y angle)
        (let [scale-x (float (or scale-x 1))
              scale-y (float (or scale-y 1))
              origin-x (float (or origin-x (/ width 2)))
              origin-y (float (or origin-y (/ height 2)))
              angle (float (or angle 0))]
          (.draw ^SpriteBatch batch object x y origin-x origin-y width height
            scale-x scale-y angle))
        (.draw ^SpriteBatch batch object x y width height)))))

(defrecord NinePatchEntity [object] Entity
  (draw-entity! [{:keys [^NinePatch object x y width height]} _ batch]
    (let [x (float (or x 0))
          y (float (or y 0))
          width (float (or width (.getTotalWidth object)))
          height (float (or height (.getTotalHeight object)))]
      (.draw object ^SpriteBatch batch x y width height))))

(defrecord ParticleEffectEntity [object] Entity
  (draw-entity! [{:keys [^ParticleEffect object x y delta-time]} _ batch]
    (let [x (float (or x 0))
          y (float (or y 0))
          ^Graphics g (Gdx/graphics)
          delta-time (float (or delta-time (.getDeltaTime g)))]
      (.setPosition object x y)
      (.draw object ^SpriteBatch batch delta-time))))

(defrecord ActorEntity [object] Entity
  (draw-entity! [{:keys [^Actor object x y width height
                         scale-x scale-y angle origin-x origin-y]} _ batch]
    (some->> x (.setX object))
    (some->> y (.setY object))
    (some->> width (.setWidth object))
    (some->> height (.setHeight object))
    (when (or scale-x scale-y angle)
      (let [scale-x (float (or scale-x 1))
            scale-y (float (or scale-y 1))
            origin-x (float (or origin-x (/ (.getWidth object) 2)))
            origin-y (float (or origin-y (/ (.getHeight object) 2)))
            angle (float (or angle 0))]
        (.setScaleX object scale-x)
        (.setScaleY object scale-y)
        (.setOriginX object origin-x)
        (.setOriginY object origin-y)
        (.setRotation object angle)))
    (.draw object ^SpriteBatch batch 1)))

(defrecord ModelEntity [object] Entity
  (draw-entity! [{:keys [^ModelInstance object x y z]}
                 {:keys [^ModelBatch renderer ^Environment attributes]}
                 _]
    (when (or x y z)
      (let [^Matrix4 m (. object transform)
            x (float (or x 0))
            y (float (or y 0))
            z (float (or z 0))]
        (.setTranslation m x y z)))
    (.render renderer object attributes)))

(defrecord ShapeEntity [object] Entity
  (draw-entity! [{:keys [^ShapeRenderer object type draw!
                         x y scale-x scale-y angle]}
                 {:keys [^Camera camera]}
                 batch]
    (when batch
      (.end ^SpriteBatch batch))
    (when camera
      (.setProjectionMatrix object (. camera combined)))
    (.begin object type)
    (when (or x y scale-x scale-y angle)
      (let [x (float (or x 0))
            y (float (or y 0))
            scale-x (float (or scale-x 1))
            scale-y (float (or scale-y 1))
            angle (float (or angle 0))]
        (.identity object)
        (.translate object x y 0)
        (.scale object scale-x scale-y 1)
        (.rotate object 0 0 1 angle)))
    (draw!)
    (.end object)
    (when batch
      (.begin ^SpriteBatch batch))))

(defrecord BundleEntity [entities] Entity
  (draw-entity! [{:keys [entities] :as entity} screen batch]
    (doseq [e entities]
      (draw-entity! (merge e entity) screen batch))))
