# record-replay
A testing framework for Spring tests allowing recording and replaying of data

This is a testing framework that allows programmatic recording/replaying of external data fetched during tests. This can be 
used to remove dependencies on external communication - and hence potential instability - from tests.
The tests are run once in recording mode to record data and then afterwards are run in replaying mode.

The framework allows tight control of what to record/replay, but also requires some code to be written and potentially some
changes of the code that needs testing.
This is because the framework functions by creating mock implementations of classes through which external communication
takes place, and then either forwards the calls to the actual implementation and records the result (in recording mode) or 
reads the recorded result from a file (in replaying mode).

Importantly, the framework is also capable of recording any asynchronous messages coming in as a result of making some external
call. This makes it possible to record quite complex exchanges.

The code includes a very simple example of how to set up the framework (the PoCTest class).

This readme is under construction and I will write a more thorough guide on how to use the framework.

## License

This software is released under the [GNU GPL V3](https://www.gnu.org/licenses/gpl-3.0.en.html) license.
