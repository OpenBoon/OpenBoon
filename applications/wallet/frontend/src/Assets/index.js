import { constants, spacing } from '../Styles'

import AssetsInfoBar from './InfoBar'
import AssetsMetadata from './Metadata'
import AssetsVisualizer from './Visualizer'

const Assets = () => {
  return (
    <div
      css={{
        height: `calc(100% - ${constants.navbar.height + 2}px)`,
        width: `calc(100% + ${spacing.spacious * 2}px)`,
        margin: -spacing.spacious,
        marginTop: 1,
      }}>
      <AssetsInfoBar />
      <div
        css={{
          display: 'flex',
          height: '100%',
          width: '100%',
        }}>
        <AssetsVisualizer />
        <AssetsMetadata />
      </div>
    </div>
  )
}

export default Assets
