package com.wse.io.test.integration.java;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import java.io.File;

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.testComplete;

public class ResizerFSTest extends TestVerticle {

	private static final String SRC_IMG = "file://src/test/resources/img.jpg";
	private EventBus eb;

	@Override
	public void start() {
		eb = vertx.eventBus();
		JsonObject config = new JsonObject();
		config.putString("address", "wse.image.resizer");
		config.putString("base-path", new File(".").getAbsolutePath());
		container.deployModule(System.getProperty("vertx.modulename"),
				config, 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> ar) {
				if (ar.succeeded()) {
					ResizerFSTest.super.start();
				} else {
					ar.cause().printStackTrace();
				}
			}
		});
	}

	@Override
	public void stop() {
		super.stop();
		vertx.fileSystem().delete("wb0x200.jpg", null);
		vertx.fileSystem().delete("wb300x0.jpg", null);
		vertx.fileSystem().delete("wb300x250.jpg", null);
		vertx.fileSystem().delete("wb300x300.jpg", null);
		vertx.fileSystem().delete("crop500x500.jpg", null);
	}

	@Test
	public void testResize() throws Exception {
		JsonObject json = new JsonObject()
				.putString("action", "resize")
				.putString("src", SRC_IMG)
				.putString("dest", "file://wb300x0.jpg")
				.putNumber("width", 300);

		eb.send("wse.image.resizer", json, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> reply) {
				assertEquals("ok", reply.body().getString("status"));
				testComplete();
			}
		});
	}

	@Test
	public void testCrop() throws Exception {
		JsonObject json = new JsonObject()
				.putString("action", "crop")
				.putString("src", SRC_IMG)
				.putString("dest", "file://crop500x500.jpg")
				.putNumber("width", 500)
				.putNumber("height", 500)
				.putNumber("x", 50)
				.putNumber("y", 100);

		eb.send("wse.image.resizer", json, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> reply) {
				assertEquals("ok", reply.body().getString("status"));
				testComplete();
			}
		});
	}

	@Test
	public void testResizeMultiple() throws Exception {
		JsonArray outputs = new JsonArray()
				.addObject(new JsonObject()
						.putString("dest", "file://wb300x300.jpg")
						.putNumber("width", 300)
						.putNumber("height", 300)
				).addObject(new JsonObject()
						.putString("dest", "file://wb300x250.jpg")
						.putNumber("width", 300)
						.putNumber("height", 250)
				).addObject(new JsonObject()
						.putString("dest", "file://wb0x200.jpg")
						.putNumber("height", 200)
				);
		JsonObject json = new JsonObject()
				.putString("action", "resizeMultiple")
				.putString("src", SRC_IMG)
				.putArray("destinations", outputs);

		eb.send("wse.image.resizer", json, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> reply) {
				assertEquals("ok", reply.body().getString("status"));
				testComplete();
			}
		});
	}

}
