/*
 * Copyright 2016 Stefan Siegl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.brokenpipe.taskermqtt;

class BundleExtraKeys {
    static final String ACTION_TYPE = "ActionType";
    static final String EVENT_SOURCE = "EventSource";
    static final String TOPIC = "Topic";
    static final String PAYLOAD = "Payload";

    static final String EVENT_SOURCE_MESSAGE_RECEIVED = "MessageReceived";
    static final String EVENT_SOURCE_CONNECTED = "Connected";
    static final String EVENT_SOURCE_DISCONNECTED = "Disconnected";

    static final String ACTION_TYPE_CONNECT = "Connect";
    static final String ACTION_TYPE_DISCONNECT = "Disconnect";
    static final String ACTION_TYPE_SUBSCRIBE = "Subscribe";
    static final String ACTION_TYPE_UNSUBSCRIBE = "Unsubscribe";
    static final String ACTION_TYPE_PUBLISH = "Publish";
}
