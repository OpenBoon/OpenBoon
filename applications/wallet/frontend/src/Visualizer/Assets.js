import { spacing } from '../Styles'

import VisualizerResize from './Resize'

const VisualizerAssets = () => {
  return (
    <div
      css={{
        padding: spacing.spacious,
        flex: 1,
        position: 'relative',
      }}>
      <VisualizerResize />
    </div>
  )
}

export default VisualizerAssets
