import { spacing } from '../Styles'

import AssetsInfoBar from './InfoBar'
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
        marginTop: 1,
        display: 'flex',
        flex: 1,
        flexDirection: 'column',
      }}>
      <AssetsInfoBar />
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
