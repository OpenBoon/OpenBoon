import { spacing } from '../Styles'

import AssetsSize from './Size'

const AssetsVisualizer = () => {
  return (
    <div
      css={{
        padding: spacing.spacious,
        flex: 1,
        position: 'relative',
      }}>
      <AssetsSize />
    </div>
  )
}

export default AssetsVisualizer
