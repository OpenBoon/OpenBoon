import { colors, constants, spacing, typography, zIndex } from '../Styles'

import InformationSvg from './information.svg'

export const WIDTH = 400
const HEADER_HEIGHT = 48
const BOX_SHADOW = '-3px 4px 3px 0px rgba(0,0,0,0.3)'

const AssetsMetadata = () => (
  <div
    css={{
      position: 'relative',
      float: 'right',
      marginRight: -spacing.spacious,
      marginBottom: -spacing.spacious,
      backgroundColor: colors.structure.mattGrey,
      height: `calc(100% + ${spacing.spacious}px)`,
      width: WIDTH,
      display: 'flex',
      flexDirection: 'column',
      boxShadow: BOX_SHADOW,
      zIndex: zIndex.layout.navbar,
    }}>
    <div
      css={{
        display: 'flex',
        height: HEADER_HEIGHT,
        alignItems: 'center',
        borderBottom: constants.borders.divider,
        paddingLeft: spacing.normal,
      }}>
      <InformationSvg width={20} />
      <div
        css={{
          paddingLeft: spacing.normal,
          color: colors.structure.steel,
          fontWeight: typography.weight.bold,
        }}>
        ASSET METADATA
      </div>
    </div>
    <div css={{ padding: spacing.normal }}>
      <div css={{ color: colors.key.one, fontStyle: typography.style.italic }}>
        Select an asset to view its metadata
      </div>
    </div>
  </div>
)

export default AssetsMetadata
