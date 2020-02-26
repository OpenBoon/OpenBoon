import { constants } from '../Styles'

import AssetsInfoBar from './InfoBar'
import AssetsMetadata from './Metadata'
import AssetsVisualizer from './Visualizer'

const FROM_TOP = constants.navbar.height + 2

const Assets = () => {
  return (
    <div css={{ height: '100%', width: '100%', marginTop: FROM_TOP }}>
      <AssetsVisualizer />
      <AssetsInfoBar />
      <AssetsMetadata />
    </div>
  )
}

export default Assets
