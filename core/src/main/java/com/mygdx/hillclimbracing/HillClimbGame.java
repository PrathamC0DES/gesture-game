package com.mygdx.hillclimbracing;

   import com.badlogic.gdx.ApplicationAdapter;
   import com.badlogic.gdx.Gdx;
   import com.badlogic.gdx.Input;
   import com.badlogic.gdx.graphics.Color;
   import com.badlogic.gdx.graphics.GL20;
   import com.badlogic.gdx.graphics.OrthographicCamera;
   import com.badlogic.gdx.graphics.Texture;
   import com.badlogic.gdx.graphics.g2d.BitmapFont;
   import com.badlogic.gdx.graphics.g2d.GlyphLayout;
   import com.badlogic.gdx.graphics.g2d.SpriteBatch;
   import com.badlogic.gdx.math.MathUtils;
   import com.badlogic.gdx.math.Rectangle;
   import com.badlogic.gdx.math.Vector2;
   import com.badlogic.gdx.math.Vector3;
   import com.badlogic.gdx.physics.box2d.*;
   import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
   import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
   import com.badlogic.gdx.utils.Array;
   import com.badlogic.gdx.utils.viewport.FitViewport;
   import com.badlogic.gdx.utils.viewport.Viewport;

   public class HillClimbGame extends ApplicationAdapter {
       private SpriteBatch batch;
       private Texture carTexture, wheelTexture, terrainTexture, respawnButtonTexture;
       private World world;
       private Box2DDebugRenderer debugRenderer;
       private Body carBody, groundBody;
       private Body leftWheelBody, rightWheelBody;
       private RevoluteJoint leftWheelJoint, rightWheelJoint;

       private float lastGeneratedX = 0;
       private final float CHUNK_WIDTH = 50f; // each new chunk width
       private final int POINTS_PER_CHUNK = 50; // number of points per chunk

       private static final float PPM = 24f;

       private boolean isCarFlipped = false;
       private final float FLIP_THRESHOLD = 0.5f;
       private Rectangle respawnButtonRect;

       private Array<Vector2> terrainVerts;

       private OrthographicCamera camera;

       private float distanceTraveled = 0;
       private float highestScore = 0;
       private float lastXPosition = 0;
       private BitmapFont scoreFont;

       private OrthographicCamera hudCamera;

       private SpriteBatch hudBatch;
       private GlyphLayout scoreTextLayout;
       private Viewport viewport;
       private static final float WORLD_WIDTH = 800;
       private static final float WORLD_HEIGHT = 480;

       private float enginePower = 300f;

       private final float CAR_WIDTH = 4.6f;
       private final float CAR_HEIGHT = 3f;
       private final float WHEEL_RADIUS = 0.56f;
       private final float WHEEL_X_OFFSET = 1.4f;
       private final float WHEEL_Y_OFFSET = -0.4f;

       private final float FLAT_START_LENGTH = 20.0f;
       private final float CAR_SPAWN_X = 5.0f;

       private final float SKY_R = 0.4f;
       private final float SKY_G = 0.6f;
       private final float SKY_B = 1.0f;

       private final float TERRAIN_R = 0.3f;
       private final float TERRAIN_G = 0.8f;
       private final float TERRAIN_B = 0.3f;

       private final float CAR_R = 0.8f;
       private final float CAR_G = 0.2f;
       private final float CAR_B = 0.2f;

       private final float WHEEL_R = 0.3f;
       private final float WHEEL_G = 0.3f;
       private final float WHEEL_B = 0.3f;

       private final float RESPAWN_R = 1.0f;
       private final float RESPAWN_G = 0.3f;
       private final float RESPAWN_B = 0.3f;

       private boolean showDebug = true;

       // Gesture control fields
       private GestureControlServer gestureServer;
       private boolean gasPressed = false;
       private boolean brakePressed = false;

       private boolean isStartupPhase = true;
       private float startupTimer = 0.5f;

       private Texture backgroundTexture, cloudTexture;
       private Array<Cloud> clouds;
       private Array<Vector2> treePositions;

       // Timer and high score variables
       private float currentTime = 0f;
       private float highestTime = 0f;
       private boolean isTimerRunning = false;
       private BitmapFont timerFont;
       private GlyphLayout timerTextLayout;

       @Override
       public void create() {
           batch = new SpriteBatch();
           hudBatch = new SpriteBatch();

           carTexture = new Texture(Gdx.files.internal("car.png"));
           wheelTexture = new Texture(Gdx.files.internal("wheel.png"));
           terrainTexture = new Texture(Gdx.files.internal("terrain.png"));
           respawnButtonTexture = new Texture(Gdx.files.internal("respawn_button.png"  ));

           backgroundTexture = new Texture(Gdx.files.internal("sky_background.png"));
           cloudTexture = new Texture(Gdx.files.internal("cloud.png"));



           hudCamera = new OrthographicCamera();
           hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
           hudBatch.setProjectionMatrix(hudCamera.combined);


           scoreFont = new BitmapFont();
           scoreFont.setColor(Color.WHITE);
           scoreFont.getData().setScale(1.0f);
           scoreTextLayout = new GlyphLayout();
           lastXPosition = CAR_SPAWN_X;

           respawnButtonRect = new Rectangle(20, 20, 80, 80);

           camera = new OrthographicCamera();
           viewport = new FitViewport(WORLD_WIDTH / PPM, WORLD_HEIGHT / PPM, camera);
           viewport.apply();
           camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);

           world = new World(new Vector2(0, -15f), true);
           debugRenderer = new Box2DDebugRenderer();

           terrainVerts = createTerrain();
           initializeDecorations();

           createCarWithWheels();

           setupContactListener();


           initGestureControl();

       }




       public void initGestureControl() {

           gestureServer = new GestureControlServer(this, 12345);
           gestureServer.start();
       }


       public void setGasPressed(boolean pressed) {
           this.gasPressed = pressed;


           if (pressed && !isTimerRunning && !isCarFlipped) {
               isTimerRunning = true;
           }
       }


       public void setBrakePressed(boolean pressed) {
           this.brakePressed = pressed;
       }


       public void respawnVehicle() {
           if (isCarFlipped) {
               respawnCar();
           }
       }


       private Texture createPlaceHolderTexture(int width, int height, float r, float g, float b) {
           com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
           pixmap.setColor(r, g, b, 1);
           pixmap.fill();
           Texture texture = new Texture(pixmap);
           pixmap.dispose();
           texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
           return texture;
       }


       private class Cloud {
           public Vector2 position;
           public float width;
           public float height;
           public float speed;

           public Cloud(float x, float y) {
               position = new Vector2(x, y);

               width = MathUtils.random(3f, 5f);
               height = MathUtils.random(1f, 2f);

               speed = MathUtils.random(0.02f, 0.05f);
           }
       }
       private void setupContactListener() {
           world.setContactListener(new ContactListener() {
               @Override
               public void beginContact(Contact contact) {

               }

               @Override
               public void endContact(Contact contact) {

               }

               @Override
               public void preSolve(Contact contact, Manifold oldManifold) {

               }

               @Override
               public void postSolve(Contact contact, ContactImpulse impulse) {

               }
           });
       }

       private void updateClouds() {
           float carX = carBody.getPosition().x;
           float cameraX = camera.position.x;
           float viewportWidth = camera.viewportWidth * camera.zoom;


           for (int i = clouds.size - 1; i >= 0; i--) {
               Cloud cloud = clouds.get(i);


               cloud.position.x += cloud.speed;


               if (cloud.position.x < cameraX - viewportWidth) {
                   clouds.removeIndex(i);
               }
           }


           if (MathUtils.random() < 0.02f) {
               float newX = cameraX + viewportWidth + MathUtils.random(0, 5);
               float newY = MathUtils.random(10, 15);
               clouds.add(new Cloud(newX, newY));
           }
       }


       private Array<Vector2> createTerrain() {
           BodyDef groundDef = new BodyDef();
           groundDef.position.set(0, 0);
           groundBody = world.createBody(groundDef);

           terrainVerts = new Array<>();


           for (int i = 0; i < 10; i++) {
               generateTerrainChunk(i * CHUNK_WIDTH, CHUNK_WIDTH);
               lastGeneratedX += CHUNK_WIDTH;
           }

           rebuildGroundBody();

           return terrainVerts;
       }

       private void initializeDecorations() {
           clouds = new Array<>();

           for (int i = 0; i < 15; i++) {
               clouds.add(new Cloud(
                   MathUtils.random(0, 200),
                   MathUtils.random(10, 15)
               ));
           }
       }

       private void generateTerrainChunk(float startX, float width) {
           float segmentWidth = width / (POINTS_PER_CHUNK - 1);
           float baseHeight = 50f / PPM;

           float lastHeight = (terrainVerts.size > 0) ? terrainVerts.peek().y : baseHeight;

           for (int i = 0; i < POINTS_PER_CHUNK; i++) {
               float x = startX + i * segmentWidth;
               float height = baseHeight;


               float distanceFromStart = x;

               float flatSectionLength = 20f;

               float transitionLength = 40f;

               if (distanceFromStart > flatSectionLength) {

                   float transitionFactor = Math.min(1.0f, (distanceFromStart - flatSectionLength) / transitionLength);

                   transitionFactor = (float) Math.pow(transitionFactor, 2); // quadratic easing


                   height += (MathUtils.sin(x * 0.8f) * 15f * transitionFactor) / PPM;
                   height += (MathUtils.sin(x * 0.3f) * 25f * transitionFactor) / PPM;
                   height += (MathUtils.sin(x * 2f) * 3f * transitionFactor) / PPM;


                   if (MathUtils.random() < 0.1f * transitionFactor) {

                       float bumpSize = MathUtils.random(2f, 8f) * transitionFactor;
                       height += bumpSize / PPM;
                   }


                   height = (lastHeight * (1.0f - transitionFactor * 0.5f)) + (height * (transitionFactor * 0.5f));


                   float randomFactor = MathUtils.random(-0.5f * transitionFactor, 0.5f * transitionFactor) / PPM;
                   height += randomFactor;
               }

               height = Math.max(height, 10f / PPM);
               terrainVerts.add(new Vector2(x, height));
               lastHeight = height;
           }
       }


       private void rebuildGroundBody() {
           if (groundBody.getFixtureList().size > 0) {
               groundBody.destroyFixture(groundBody.getFixtureList().first());
           }


           Array<Vector2> filteredVerts = new Array<>();
           if (terrainVerts.size > 0) {
               filteredVerts.add(terrainVerts.first());

               for (int i = 1; i < terrainVerts.size; i++) {
                   Vector2 prevVert = filteredVerts.peek();
                   Vector2 currentVert = terrainVerts.get(i);


                   float distSquared = Vector2.dst2(prevVert.x, prevVert.y, currentVert.x, currentVert.y);


                   if (distSquared > 0.006f * 0.006f) {
                       filteredVerts.add(currentVert);
                   }
               }
           }


           if (filteredVerts.size < 2) {
               System.out.println("Warning: Not enough vertices to create chain shape after filtering");
               return;
           }

           ChainShape groundShape = new ChainShape();
           groundShape.createChain(filteredVerts.toArray(Vector2.class));

           FixtureDef fixtureDef = new FixtureDef();
           fixtureDef.shape = groundShape;
           fixtureDef.friction = 1.5f;
           fixtureDef.restitution = 0.1f;

           groundBody.createFixture(fixtureDef);
           groundShape.dispose();
       }



       private Vector2[] generateTerrain(int points, float width, float baseHeight) {
           Vector2[] vertices = new Vector2[points];

           float segmentWidth = width / (points - 1);
           float lastHeight = baseHeight / PPM;

           int flatStartVertices = (int)(FLAT_START_LENGTH / (segmentWidth / PPM));

           for (int i = 0; i < points; i++) {
               float x = i * segmentWidth / PPM;
               float height;

               if (i < flatStartVertices) {
                   height = baseHeight / PPM;
               } else {

                   height = baseHeight / PPM;

                   height += (MathUtils.sin((x - (flatStartVertices * segmentWidth / PPM)) * 0.8f) * 15f) / PPM;
                   height += (MathUtils.sin((x - (flatStartVertices * segmentWidth / PPM)) * 0.3f) * 25f) / PPM;
                   height += (MathUtils.sin((x - (flatStartVertices * segmentWidth / PPM)) * 2f) * 3f) / PPM;

                   if (x > (flatStartVertices * segmentWidth / PPM) + 5 && MathUtils.random() < 0.08 && i > flatStartVertices) {

                       float hillHeight = MathUtils.random(10f, 20f) / PPM;
                       float hillWidth = MathUtils.random(5, 15);

                       for (int j = 0; j < hillWidth && i + j < points; j++) {

                           float factor = (float) Math.sin((j / hillWidth) * Math.PI);

                           if (j == 0) {
                               height += hillHeight * factor;
                           }
                       }
                   }

                   if (i > flatStartVertices) {
                       height = lastHeight * 0.5f + height * 0.5f;
                   }

                   height += MathUtils.random(-0.5f, 0.5f) / PPM;
               }

               height = Math.max(height, 10f / PPM);
               lastHeight = height;

               vertices[i] = new Vector2(x, height);
           }

           return vertices;
       }

       private void createCarWithWheels() {
           float spawnY = getTerrainHeightAt(CAR_SPAWN_X) + WHEEL_RADIUS + 2f;

           BodyDef carDef = new BodyDef();
           carDef.type = BodyDef.BodyType.DynamicBody;
           carDef.position.set(CAR_SPAWN_X, spawnY);
           carBody = world.createBody(carDef);

           PolygonShape carShape = new PolygonShape();
           carShape.setAsBox(CAR_WIDTH / 2.7f, CAR_HEIGHT /6);

           FixtureDef carFixture = new FixtureDef();
           carFixture.shape = carShape;
           carFixture.density = 22.0f;
           carFixture.friction = 0.6f;
           carFixture.restitution = 0.1f;
           carBody.createFixture(carFixture);
           carShape.dispose();

           createWheel(true);
           createWheel(false);
       }
       private float getTerrainHeightAt(float x) {
           if (terrainVerts == null || terrainVerts.size < 2) {
               return 4f; // Default height if no terrain exists
           }

           // Handle case where x is before the first terrain vertex
           if (x < terrainVerts.first().x) {
               return terrainVerts.first().y;
           }


           if (x > terrainVerts.peek().x) {
               return terrainVerts.peek().y;
           }


           for (int i = 0; i < terrainVerts.size - 1; i++) {
               Vector2 v1 = terrainVerts.get(i);
               Vector2 v2 = terrainVerts.get(i + 1);

               if (v1.x <= x && v2.x >= x) {

                   float t = (x - v1.x) / (v2.x - v1.x);
                   return v1.y + t * (v2.y - v1.y);
               }
           }


           return 4f;
       }

       private void createWheel(boolean isLeftWheel) {

           float xOffset = isLeftWheel ? -WHEEL_X_OFFSET : WHEEL_X_OFFSET;

           BodyDef wheelDef = new BodyDef();
           wheelDef.type = BodyDef.BodyType.DynamicBody;
           wheelDef.position.set(carBody.getPosition().x + xOffset, carBody.getPosition().y + WHEEL_Y_OFFSET);

           Body wheelBody = world.createBody(wheelDef);

           CircleShape wheelShape = new CircleShape();
           wheelShape.setRadius(WHEEL_RADIUS);

           FixtureDef wheelFixture = new FixtureDef();
           wheelFixture.shape = wheelShape;
           wheelFixture.density = 6.0f;
           wheelFixture.friction = 2.0f;
           wheelFixture.restitution = 0.1f;

           wheelBody.createFixture(wheelFixture);
           wheelShape.dispose();

           RevoluteJointDef jointDef = new RevoluteJointDef();
           jointDef.bodyA = carBody;
           jointDef.bodyB = wheelBody;
           jointDef.localAnchorA.set(xOffset, WHEEL_Y_OFFSET);
           jointDef.localAnchorB.set(0, 0);
           jointDef.enableMotor = true;
           jointDef.maxMotorTorque = 200f;
           jointDef.motorSpeed = 0;

           RevoluteJoint joint = (RevoluteJoint) world.createJoint(jointDef);

           if (isLeftWheel) {
               leftWheelBody = wheelBody;
               leftWheelJoint = joint;
           } else {
               rightWheelBody = wheelBody;
               rightWheelJoint = joint;
           }
       }

       private void update() {
           float deltaTime = Gdx.graphics.getDeltaTime();

           if (isStartupPhase) {
               startupTimer -= deltaTime;
               if (startupTimer <= 0) {
                   isStartupPhase = false;
                   // Make sure everything is still
                   carBody.setLinearVelocity(0, 0);
                   carBody.setAngularVelocity(0);
                   leftWheelBody.setLinearVelocity(0, 0);
                   leftWheelBody.setAngularVelocity(0);
                   rightWheelBody.setLinearVelocity(0, 0);
                   rightWheelBody.setAngularVelocity(0);
                   leftWheelJoint.setMotorSpeed(0);
                   rightWheelJoint.setMotorSpeed(0);
               }
           }


           world.step(deltaTime, 6, 2);


           checkAndGenerateMoreTerrain();



           boolean wasFlipped = isCarFlipped;
           isCarFlipped = checkIfCarFlipped();

           // Update score
           updateScore();

           handleInput();

           updateClouds();

           updateCamera();

           if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
               showDebug = !showDebug;
           }

           if (isCarFlipped && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
               respawnCar();
           }

           if (isCarFlipped && Gdx.input.justTouched()) {
               Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
               camera.unproject(touchPos);
               if (respawnButtonRect.contains(touchPos.x, touchPos.y)) {
                   respawnCar();
               }
           }
       }


       private void checkAndGenerateMoreTerrain() {
           float carX = carBody.getPosition().x;


           if (carX > lastGeneratedX - 50) {

               for (int i = 0; i < 3; i++) {
                   generateTerrainChunk(lastGeneratedX, CHUNK_WIDTH);

                   for (int j = 0; j < POINTS_PER_CHUNK; j += 5) {
                       int index = terrainVerts.size - POINTS_PER_CHUNK + j;
                       if (index >= 0 && MathUtils.random() < 0.3f) {
                           treePositions.add(new Vector2(
                               terrainVerts.get(index).x,
                               terrainVerts.get(index).y + 0.5f
                           ));
                       }
                   }

                   lastGeneratedX += CHUNK_WIDTH;
               }


               float removeThreshold = carX - 100;
               while (terrainVerts.size > 2 && terrainVerts.first().x < removeThreshold) {
                   terrainVerts.removeIndex(0);
               }

               for (int i = treePositions.size - 1; i >= 0; i--) {
                   if (treePositions.get(i).x < removeThreshold) {
                       treePositions.removeIndex(i);
                   }
               }

               rebuildGroundBody();
           }
       }


       private void updateScore() {

           float currentXPosition = carBody.getPosition().x;


           if (!isCarFlipped && currentXPosition > lastXPosition) {

               distanceTraveled += (currentXPosition - lastXPosition) * 10; // Multiply by 10 to make score increase faster
           }


           lastXPosition = currentXPosition;


           if (distanceTraveled > highestScore) {
               highestScore = distanceTraveled;
           }
       }

       private boolean checkIfCarFlipped() {

           float rotation = carBody.getAngle() % (2 * MathUtils.PI);

           return Math.abs(rotation) > MathUtils.PI - FLIP_THRESHOLD;
       }

       private void respawnCar() {

           float spawnX = CAR_SPAWN_X;


           if (terrainVerts == null || terrainVerts.size < 2) {
               System.out.println("Warning: No terrain data for respawn");
               spawnX = 5.0f;
           } else {

               if (spawnX < terrainVerts.first().x) {
                   spawnX = terrainVerts.first().x + 2.0f;
               }
               if (spawnX > terrainVerts.peek().x - 5.0f) {
                   spawnX = terrainVerts.peek().x - 5.0f;
               }
           }


           float spawnY = getTerrainHeightAt(spawnX) + WHEEL_RADIUS + 1.0f;


           System.out.println("Respawning car at: " + spawnX + ", " + spawnY);

           // Set car and wheel positions
           carBody.setTransform(spawnX, spawnY, 0);
           carBody.setLinearVelocity(0, 0);
           carBody.setAngularVelocity(0);

           leftWheelBody.setTransform(spawnX - WHEEL_X_OFFSET, spawnY + WHEEL_Y_OFFSET, 0);
           leftWheelBody.setLinearVelocity(0, 0);
           leftWheelBody.setAngularVelocity(0);

           rightWheelBody.setTransform(spawnX + WHEEL_X_OFFSET, spawnY + WHEEL_Y_OFFSET, 0);
           rightWheelBody.setLinearVelocity(0, 0);
           rightWheelBody.setAngularVelocity(0);

           // Reset game state
           isCarFlipped = false;
           currentTime = 0f;
           distanceTraveled = 0f;
           lastXPosition = spawnX;


           for (int i = 0; i < 5; i++) {
               world.step(1/60f, 6, 2);
           }
       }

       private void handleInput() {

           boolean keyPressed = false;

           // Handle keyboard input
           boolean keyboardGas = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
           boolean keyboardBrake = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);


           boolean accelerate = keyboardGas || gasPressed;
           boolean brake = keyboardBrake || brakePressed;


           if ((keyboardGas || gasPressed) && !isTimerRunning && !isCarFlipped) {
               isTimerRunning = true;
           }

           float leftWheelVelocity = leftWheelBody.getAngularVelocity();
           float rightWheelVelocity = rightWheelBody.getAngularVelocity();
           float averageWheelVelocity = (leftWheelVelocity + rightWheelVelocity) / 2;

           if (accelerate) {
               keyPressed = true;
               leftWheelJoint.setMotorSpeed(-enginePower);
               rightWheelJoint.setMotorSpeed(-enginePower);
           }

           if (brake) {
               keyPressed = true;


               if (averageWheelVelocity < -0.5f) {

                   leftWheelJoint.setMotorSpeed(0);
                   rightWheelJoint.setMotorSpeed(0);
                   leftWheelBody.setAngularVelocity(leftWheelVelocity * 0.9f);  // Slow down wheels
                   rightWheelBody.setAngularVelocity(rightWheelVelocity * 0.9f);
               }

               else if (averageWheelVelocity >= -0.5f) {

                   float reversePower = Math.min(enginePower, enginePower * (0.5f + Math.abs(averageWheelVelocity)));
                   leftWheelJoint.setMotorSpeed(reversePower);
                   rightWheelJoint.setMotorSpeed(reversePower);
               }
           }

           if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
               keyPressed = true;
               leftWheelJoint.setMotorSpeed(0);
               rightWheelJoint.setMotorSpeed(0);
               leftWheelBody.setAngularVelocity(leftWheelBody.getAngularVelocity() * 0.8f);
               rightWheelBody.setAngularVelocity(rightWheelBody.getAngularVelocity() * 0.8f);
           }

           if (!keyPressed) {
               leftWheelJoint.setMotorSpeed(0);
               rightWheelJoint.setMotorSpeed(0);

               leftWheelBody.setAngularVelocity(leftWheelBody.getAngularVelocity() * 0.98f);
               rightWheelBody.setAngularVelocity(rightWheelBody.getAngularVelocity() * 0.98f);
           }
       }

       private void updateCamera() {

           Vector2 carPosition = carBody.getPosition();

           float lookAheadX = carBody.getLinearVelocity().x * 0.7f;

           camera.position.x = camera.position.x + (carPosition.x + lookAheadX - camera.position.x) * 0.1f;
           float targetY = carPosition.y + 3f;
           camera.position.y = camera.position.y + (targetY - camera.position.y) * 0.1f;

           camera.position.y = Math.max(camera.position.y, 3f);

           camera.zoom = 1.2f;

           camera.update();
       }

       @Override
       public void render() {
           update();

           Gdx.gl.glClearColor(SKY_R, SKY_G, SKY_B, 1);
           Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

           batch.setProjectionMatrix(camera.combined);

           batch.begin();

           drawBackground();
           drawTerrain();


           drawCar();

           drawWheel(batch, leftWheelBody);
           drawWheel(batch, rightWheelBody);

           if (isCarFlipped) {

               float buttonX = camera.position.x - camera.viewportWidth * camera.zoom / 2 + 1.0f;
               float buttonY = camera.position.y - camera.viewportHeight * camera.zoom / 2 + 1.0f;
               batch.draw(respawnButtonTexture, buttonX, buttonY, 3.0f, 3.0f);

               respawnButtonRect.set(buttonX, buttonY, 3.0f, 3.0f);
           }


           drawScore();

           batch.end();

           if (showDebug) {
               debugRenderer.render(world, camera.combined);
           }
       }

       private void drawScore() {

           batch.end();


           hudBatch.begin();


           String currentScoreStr = String.format("%06d", (int)distanceTraveled);
           String highScoreStr = String.format("HI %06d", (int)highestScore);


           float screenWidth = Gdx.graphics.getWidth();
           float screenHeight = Gdx.graphics.getHeight();


           scoreFont.setColor(1.0f, 0.8f, 0.2f, 1.0f);  // Gold color
           scoreFont.draw(hudBatch, highScoreStr, 20, screenHeight - 20);


           scoreFont.setColor(1.0f, 1.0f, 1.0f, 1.0f);  // White color
           scoreFont.draw(hudBatch, currentScoreStr, 20, screenHeight - 50);


           hudBatch.end();


           batch.begin();
       }


       private String formatTime(float timeInSeconds) {
           int minutes = (int)(timeInSeconds / 60);
           int seconds = (int)(timeInSeconds % 60);
           int tenths = (int)((timeInSeconds - (int)timeInSeconds) * 10);

           if (minutes > 0) {
               return String.format("%d:%02d", minutes, seconds);
           } else {
               return String.format("%d.%01d", seconds, tenths);
           }
       }

        private void drawTerrain() {
            if (terrainVerts != null && terrainVerts.size > 1) {

                for (int i = 0; i < terrainVerts.size - 1; i++) {
                    Vector2 v1 = terrainVerts.get(i);
                    Vector2 v2 = terrainVerts.get(i + 1);


                    float bottomY = camera.position.y - camera.viewportHeight * camera.zoom / 2 - 1.0f;


                    batch.setColor(TERRAIN_R, TERRAIN_G, TERRAIN_B, 1.0f);


                    batch.draw(terrainTexture,
                        v1.x, bottomY,  // Bottom left
                        0, 0,
                        v2.x - v1.x, v1.y - bottomY,
                        1, 1,
                        0,
                        0, 0,
                        1, 1,
                        false, false);


                    if (v1.y != v2.y) {
                        batch.draw(terrainTexture,
                            v1.x, Math.min(v1.y, v2.y),  // Bottom left
                            0, 0,
                            v2.x - v1.x, Math.abs(v2.y - v1.y),  // Width and height
                            1, 1,
                            0,
                            0, 0,
                            1, 1,
                            false, false);
                    }
                }


                batch.setColor(0.0f, 0.7f, 0.2f, 1.0f); // Brighter green for grass
                for (int i = 0; i < terrainVerts.size - 1; i++) {
                    Vector2 v1 = terrainVerts.get(i);
                    Vector2 v2 = terrainVerts.get(i + 1);

                    float grassThickness = 0.4f;


                    float angle = MathUtils.atan2(v2.y - v1.y, v2.x - v1.x) * MathUtils.radiansToDegrees;
                    float length = v2.x - v1.x;


                    batch.draw(terrainTexture,
                        v1.x, v1.y,
                        0, grassThickness/2,
                        length + 0.05f, grassThickness,
                        1, 1,
                        angle,
                        0, 0,
                        terrainTexture.getWidth(), terrainTexture.getHeight(),
                        false, false);
                }
                batch.setColor(Color.WHITE);
        }
}

       private void drawBackground() {

           float width = camera.viewportWidth * camera.zoom * 1.5f;
           float height = camera.viewportHeight * camera.zoom * 1.5f;
           batch.draw(backgroundTexture,
                      camera.position.x - width/2,
                      camera.position.y - height/2,
                      width, height);


           for (Cloud cloud : clouds) {
               batch.draw(cloudTexture,
                          cloud.position.x,
                          cloud.position.y,
                          cloud.width,
                          cloud.height);
           }
       }

       private void drawCar() {
           float carAngle = carBody.getAngle() * MathUtils.radiansToDegrees;

           float visualHeight = CAR_HEIGHT * 1.5f;
           float heightOffset = (visualHeight - CAR_HEIGHT) / 2;

           batch.draw(carTexture,
                   carBody.getPosition().x - CAR_WIDTH/2,
                   carBody.getPosition().y - CAR_HEIGHT/2 - heightOffset +0.2f ,
                   CAR_WIDTH/2, visualHeight/2,
                   CAR_WIDTH, visualHeight,
                   1, 1,
                   carAngle,
                   0, 0,
                   carTexture.getWidth(), carTexture.getHeight(),
                   false, false);
       }

       private void drawWheel(SpriteBatch batch, Body wheelBody) {
           float diameter = WHEEL_RADIUS * 2;
           float wheelAngle = wheelBody.getAngle() * MathUtils.radiansToDegrees;

           batch.draw(wheelTexture,
                   wheelBody.getPosition().x - WHEEL_RADIUS,
                   wheelBody.getPosition().y - WHEEL_RADIUS,
                   WHEEL_RADIUS, WHEEL_RADIUS,
                   diameter, diameter,
                   1, 1,
                   wheelAngle,
                   0, 0,
                   wheelTexture.getWidth(), wheelTexture.getHeight(),
                   false, false);
       }

       @Override
       public void resize(int width, int height) {
           viewport.update(width, height);
           camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);

           if (hudCamera != null) {
               hudCamera.setToOrtho(false, width, height);
               hudCamera.update();
               hudBatch.setProjectionMatrix(hudCamera.combined);
           }
       }

       @Override
       public void dispose() {

           if (gestureServer != null) {
               gestureServer.stop();
           }

           batch.dispose();
           if (carTexture != null) carTexture.dispose();
           if (wheelTexture != null) wheelTexture.dispose();
           if (hudBatch != null) hudBatch.dispose();
           if (terrainTexture != null) terrainTexture.dispose();
           if (respawnButtonTexture != null) respawnButtonTexture.dispose();

           if (backgroundTexture != null) backgroundTexture.dispose();
           if (cloudTexture != null) cloudTexture.dispose();

           if (scoreFont != null) scoreFont.dispose();
           world.dispose();
           debugRenderer.dispose();
       }
   }
