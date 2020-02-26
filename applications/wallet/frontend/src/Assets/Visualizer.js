import { spacing } from '../Styles'

import { WIDTH as METADATA_WIDTH } from './Metadata'

const AssetsVisualizer = () => {
  return (
    <div
      css={{
        float: 'left',
        paddingTop: spacing.spacious,
        height: '100%',
        width: `calc(100% - ${METADATA_WIDTH}px)`,
        margin: 0,
      }}
    />
  )
}

export default AssetsVisualizer
