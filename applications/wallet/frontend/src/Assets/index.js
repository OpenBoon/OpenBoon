import { spacing } from '../Styles'

import AssetsInfobar from './Infobar'
import AssetsMetadata from './Metadata'
import AssetsVisualizer from './Visualizer'

const Assets = () => {
  return (
    <div
      css={{
        height: '100%',
        marginLeft: -spacing.spacious,
        marginRight: -spacing.spacious,
        marginBottom: -spacing.spacious,
        marginTop: spacing.hairline,
        display: 'flex',
        flex: 1,
        flexDirection: 'column',
      }}>
      <AssetsInfobar />
      <div
        css={{
          display: 'flex',
          height: '100%',
        }}>
        <AssetsVisualizer />
        <AssetsMetadata />
      </div>
    </div>
  )
}

export default Assets
