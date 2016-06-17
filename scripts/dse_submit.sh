#!/bin/bash

dse spark-submit --class com.rootedinsights.loggerhead.EventShovel --verbose target/scala-2.10/event_shovel.jar
