export const getFinalSize = ({
  startingSize,
  newSize,
  minExpandedSize,
  minCollapsedSize,
}) => {
  if (startingSize && newSize < minExpandedSize) {
    // snap down
    if (newSize < startingSize) {
      return minCollapsedSize
    }

    // snap up
    return minExpandedSize
  }

  return newSize
}
