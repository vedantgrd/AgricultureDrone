import cv2
import numpy as np

def compute_health_index(image):
    B, G, R = cv2.split(image)
    R = R.astype(float)
    G = G.astype(float)
    B = B.astype(float)
    
    # Calculate health index
    health_index = G - (R + B) / 2
    return health_index

def create_color_map(health_index, image):
    height, width = health_index.shape
    color_image = np.zeros((height, width, 3), dtype=np.uint8)

    # Convert image to HSV color space
    hsv_image = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)

    # Set thresholds based on observations
    healthy_threshold = 50  
    unhealthy_threshold = 10  

    for i in range(height):
        for j in range(width):
            # Get HSV values
            h, s, v = hsv_image[i, j]

            # Check for blue or white pixels (sky)
            if (h >= 100 and h <= 140 and s >= 50):  # Blue hue range
                color_image[i, j] = [255, 0, 0]  # Blue for sky
            elif (v > 200 and s < 30):  # White condition (high value, low saturation)
                color_image[i, j] = [255, 0, 0]  # Blue for sky
            elif health_index[i, j] > healthy_threshold:
                color_image[i, j] = [0, 255, 0]  # Green for healthy crops
            elif health_index[i, j] < unhealthy_threshold:
                color_image[i, j] = [0, 0, 255]  # Red for unhealthy crops
            else:
                color_image[i, j] = [0, 0, 0]      # Black for neutral areas

    return color_image

def main():
    image_path = 'C:\\Users\\vedan\\Desktop\\AgricultureDrone\\algo\\test1.jpg'  # Update this path as needed
    image = cv2.imread(image_path)

    if image is None:
        print("Error: Could not read the image.")
        return

    # Compute health index
    health_index = compute_health_index(image)

    # Create color map based on health index and original image
    color_map_image = create_color_map(health_index, image)

    # Display only the Health Index Color Map
    cv2.imshow('Health Index Color Map', color_map_image)
    
    cv2.waitKey(0)
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()