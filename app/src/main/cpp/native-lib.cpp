#include <jni.h>
#include <string>
#include "Board.h"
#include "AIWine.h"


AIWine *ai;

extern "C"
JNIEXPORT void JNICALL
Java_wine_Wine_move(JNIEnv *env, jclass, jint row, jint col) {
    ai->turnMove(row, col);
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_wine_Wine_best(JNIEnv *env, jclass clazz) {
    int x;
    int y;
    ai->turnBest(x, y);
    int point[] = {x, y};
    jintArray array = env->NewIntArray(2);
    env->SetIntArrayRegion(array, 0, 2, point);
    return array;
}


extern "C"
JNIEXPORT void JNICALL
Java_wine_Wine_init(JNIEnv *env, jclass) {
    delete(ai);
    ai = new AIWine();
    ai->setSize(15);
}

