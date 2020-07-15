# TensorFlow and tf.keras
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

# Helper libraries
import numpy as np
import librosa as lr
import os
import matplotlib.pyplot as plt

import time

print(tf.__version__)
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"


def main_func():
    start = time.time()
    audio_path = './data/ted01.wav'
    x, sr = lr.load(audio_path)
    X = lr.stft(x, n_fft=512)
    Xdb = lr.amplitude_to_db(abs(X))
    Xdb = Xdb.reshape((39193, 5, 257))
    Y = np.random.randint(1, 26, 39193)

    model = keras.Sequential([
        layers.LSTM(128, input_shape=(5, 257)),
        layers.BatchNormalization(),
        layers.Dense(128, activation='relu'),
        layers.Dense(26, activation='softmax')
    ])

    model.compile(optimizer='adam',
                  loss='sparse_categorical_crossentropy',
                  metrics=['accuracy'])

    model.fit(Xdb, Y, batch_size=64, epochs=100)
    end = time.time()
    print('spend time: {}s'.format(end - start))


if __name__ == '__main__':
    main_func()
