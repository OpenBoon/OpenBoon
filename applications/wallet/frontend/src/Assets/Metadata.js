import { colors, spacing, typography, zIndex } from '../Styles'

import InformationSvg from './information.svg'

const WIDTH = 400
const HEADER_HEIGHT = 48
const FROM_TOP = 90
const BOX_SHADOW = '-3px 4px 3px 0px rgba(0,0,0,0.3)'

const AssetMetadata = () => (
  <div
    css={{
      position: 'fixed',
      top: FROM_TOP,
      right: 0,
      bottom: 0,
      backgroundColor: colors.structure.mattGrey,
      height: '100%',
      width: WIDTH,
      display: 'flex',
      flexDirection: 'column',
      boxShadow: BOX_SHADOW,
      zIndex: zIndex.layout.navbar + 1,
    }}>
    <div
      css={{
        display: 'flex',
        height: HEADER_HEIGHT,
        alignItems: 'center',
        borderBottom: `1px solid ${colors.structure.smoke}`,
        paddingLeft: spacing.normal,
      }}>
      <InformationSvg />
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

export default AssetMetadata
