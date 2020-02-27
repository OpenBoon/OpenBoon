import { spacing } from '../Styles'

import VisualizerInfobar from './Infobar'
import VisualizerMetadata from './Metadata'
import VisualizerAssets from './Assets'

const Visualizer = () => {
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
      <VisualizerInfobar />
      <div
        css={{
          display: 'flex',
          height: '100%',
        }}>
        <VisualizerAssets />
        <VisualizerMetadata />
      </div>
    </div>
  )
}

export default Visualizer
