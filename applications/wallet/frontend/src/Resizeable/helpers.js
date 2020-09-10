export const getFinalSize = ({
  startingAxis,
  sizeCalculation,
  minExpandedSize,
  minCollapsedSize,
}) => {
  if (startingAxis && sizeCalculation < minExpandedSize) {
    // snap down
    if (sizeCalculation < startingAxis) {
      return minCollapsedSize
    }

    // snap up
    if (sizeCalculation > startingAxis) {
      return minExpandedSize
    }
  }

  return sizeCalculation
}
