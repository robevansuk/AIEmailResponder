## AI Email Auto Responder

After hearing how AI can respond to emails for you I asked Claude 3.7 to generate for me an AI Auto Responder.
This is what it implemented (almost entirely).

### Where Claude fell short

- Dependencies in the `build.gradle` I had to compile myself with relevant (more up to date) versions
- the OpenAiResponse did not ignore unknown fields in the json so I had to fully flesh this out manually
- Initially I used gpt-4 model which is expensive. I've instead reverted to a considerably cheaper (slightly slower) gpt-4o model
- The code was provided to integrate with outlook, but I've disabled this as I don't have access to an outlook account for testing.