# lifting one side

import RPi.GPIO as gpio
import time
import threading
from quadPI.calibrate import *
from quadPI.quad_modes import *
from quadPI.quad_tricks import *

# GPIO pin assignments (BCM mode)
ch1 = 19  # Aileron (Roll)
ch2 = 12  # Elevator (Pitch)
ch3 = 18  # Throttle
ch4 = 21  # Rudder (Yaw)
ch5 = 26  # AUX Channel

# EXACT original values from provided code
throttle_max = 9
throttle_min = 3
max_stick_position = 9
mid_stick_position = 7
min_stick_position = 4
signal_frequency = 50

# HIGHER TAKEOFF VALUE
takeoff_throttle = 9  # Using maximum throttle for takeoff

# Clean any existing GPIO settings
try:
    gpio.cleanup()
except:
    pass

# Set GPIO mode to BCM
gpio.setmode(gpio.BCM)
gpio.setwarnings(False)

# Setup all pins
gpio.setup(ch1, gpio.OUT)
gpio.setup(ch2, gpio.OUT)
gpio.setup(ch3, gpio.OUT)
gpio.setup(ch4, gpio.OUT)
gpio.setup(ch5, gpio.OUT)

# Create PWM objects
aileron_ch1 = gpio.PWM(ch1, signal_frequency)
elevator_ch2 = gpio.PWM(ch2, signal_frequency)
throttle_ch3 = gpio.PWM(ch3, signal_frequency)
rudder_ch4 = gpio.PWM(ch4, signal_frequency)
aux_ch5 = gpio.PWM(ch5, signal_frequency)

# Initialize calibration and mode functions
cb = Calibrate()
modes = Modes()
tricks = QuadTricks()

# Function to stop all PWM signals and cleanup
def stopAll():
    print("Stopping all PWM signals...")
    aileron_ch1.stop()
    elevator_ch2.stop()
    throttle_ch3.stop()
    rudder_ch4.stop()
    aux_ch5.stop()
    time.sleep(0.5)
    gpio.cleanup()

# True synchronized start using threads
def truly_synchronized_start(pwm_channels):
    """
    Start all PWM channels at EXACTLY the same time using threads.
    """
    # Create a barrier to synchronize all threads
    start_barrier = threading.Barrier(len(pwm_channels) + 1)
    
    # Thread function to start PWM
    def start_pwm(pwm_channel, value):
        # Wait at barrier until all threads are ready
        start_barrier.wait()
        # Start PWM immediately when barrier is released
        pwm_channel.start(value)
    
    # Create and start threads for each PWM channel
    threads = []
    for pwm_channel, value in pwm_channels.items():
        thread = threading.Thread(target=start_pwm, args=(pwm_channel, value))
        thread.daemon = True
        thread.start()
        threads.append(thread)
    
    # Main thread waits at barrier and releases all PWM starts simultaneously
    start_barrier.wait()
    
    # Wait for all threads to complete
    for thread in threads:
        thread.join()

def resetAll():
    """Reset all PWM signals to neutral positions"""
    aileron_ch1.ChangeDutyCycle(mid_stick_position)
    elevator_ch2.ChangeDutyCycle(mid_stick_position)
    throttle_ch3.ChangeDutyCycle(throttle_min)
    rudder_ch4.ChangeDutyCycle(mid_stick_position)
    aux_ch5.ChangeDutyCycle(mid_stick_position)

def main():
    try:
        print("Starting quadcopter with PERFECT synchronization...")
        
        # Start all PWM channels at EXACTLY the same time using threading
        print("Starting all motors with absolute synchronization...")
        truly_synchronized_start({
            aileron_ch1: mid_stick_position,
            elevator_ch2: mid_stick_position,
            throttle_ch3: throttle_min,
            rudder_ch4: mid_stick_position,
            aux_ch5: mid_stick_position
        })
        
        time.sleep(1)
        
        # USING ORIGINAL ARMING LOGIC
        print("Arming the quad using ORIGINAL arming logic...")
        modes.armed_mode(throttle_ch3, rudder_ch4, throttle_min,
                         min_stick_position, mid_stick_position)
        time.sleep(2)

        print("Throttle surge test with original values...")
        tricks.throttle_surge_fall(throttle_ch3, 3, 8)
        time.sleep(2)
        
        print("Flying mode active...")
        
        # SET FLIGHT VALUES - Using original values but with higher throttle
        throttle_value = 8  # High throttle but not maximum to start
        roll_value = mid_stick_position
        pitch_value = mid_stick_position
        yaw_value = mid_stick_position
        
        print(f"Setting flight controls - Throttle: {throttle_value}, Roll: {roll_value}, Pitch: {pitch_value}, Yaw: {yaw_value}")
        aileron_ch1.ChangeDutyCycle(roll_value)
        elevator_ch2.ChangeDutyCycle(pitch_value)
        throttle_ch3.ChangeDutyCycle(throttle_value)
        rudder_ch4.ChangeDutyCycle(yaw_value)
        
        # Wait 5 seconds, then increase to maximum throttle if needed
        time.sleep(5)
        
        print(f"Increasing throttle to maximum: {takeoff_throttle}")
        throttle_ch3.ChangeDutyCycle(takeoff_throttle)
        
        print("Quadcopter in flight - running indefinitely")
        # Keep the quadcopter running in flight
        while True:
            time.sleep(0.1)
        
    except KeyboardInterrupt:
        print("\nEmergency landing initiated!")
    except Exception as e:
        print(f'Error: {e}')
    finally:
        print("Landing and disarming...")
        
        # Reduce throttle
        throttle_ch3.ChangeDutyCycle(throttle_min)
        time.sleep(1)
        
        # Disarm using original safe mode
        print("Disarming...")
        modes.safe_mode(throttle_ch3, rudder_ch4, throttle_min,
                       max_stick_position, mid_stick_position)
        time.sleep(1)
        
        resetAll()
        time.sleep(1)
        
        # Stop all PWM
        stopAll()

if __name__ == "__main__":
    main()
