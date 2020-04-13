import { colors, spacing } from '../Styles'

import Assets from '../Assets'
import Metadata from '../Metadata'

const VisualizerContent = () => {
  return (
    <div
      css={{
        height: '100%',
        backgroundColor: colors.structure.coal,
        marginLeft: -spacing.spacious,
        marginRight: -spacing.spacious,
        marginBottom: -spacing.spacious,
        marginTop: spacing.hairline,
        display: 'flex',
        flex: 1,
        flexDirection: 'column',
      }}
    >
      <div css={{ display: 'flex', height: '100%', overflowY: 'hidden' }}>
        <Assets />
        <Metadata />
      </div>
    </div>
  )
}

export default VisualizerContent
