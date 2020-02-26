import { colors, constants, spacing, typography, zIndex } from '../Styles'

import InformationSvg from './information.svg'

export const WIDTH = 400
const HEADER_HEIGHT = 48

const AssetsMetadata = () => (
  <div css={{ paddingTop: 1 }}>
    <div
      css={{
        alignSelf: 'flex-end',
        marginRight: -spacing.spacious,
        marginBottom: -spacing.spacious,
        backgroundColor: colors.structure.mattGrey,
        height: `calc(100% + ${spacing.spacious}px)`,
        width: WIDTH,
        display: 'flex',
        flexDirection: 'column',
        boxShadow: constants.boxShadows.metadata,
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
        <InformationSvg width={20} color={colors.structure.steel} />
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
        <div
          css={{ color: colors.key.one, fontStyle: typography.style.italic }}>
          Select an asset to view its metadata
        </div>
      </div>
    </div>
  </div>
)

export default AssetsMetadata
