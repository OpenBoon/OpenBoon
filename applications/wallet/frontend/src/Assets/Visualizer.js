import { spacing } from '../Styles'

import AssetsResize from './Resize'

const AssetsVisualizer = () => {
  return (
    <div
      css={{
        padding: spacing.spacious,
        flex: 1,
        position: 'relative',
      }}>
      <AssetsResize />
    </div>
  )
}

export default AssetsVisualizer
