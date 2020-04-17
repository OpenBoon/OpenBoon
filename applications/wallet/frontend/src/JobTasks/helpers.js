export const getDuration = ({ timeStarted, timeStopped, now }) => {
  const stopTime = timeStopped < 0 ? now : timeStopped
  return stopTime - timeStarted
}
