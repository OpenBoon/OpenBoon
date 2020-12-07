import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import BackSvg from '../Icons/back.svg'

const AssetNavigation = ({ projectId, assetId, query, filename }) => {
  const idString = `?assetId=${assetId}`
  const queryString = query ? `&query=${query}` : ''

  return (
    <div
      css={{
        paddingLeft: spacing.base,
        paddingRight: spacing.base,
        backgroundColor: colors.structure.lead,
        color: colors.structure.steel,
        marginBottom: spacing.hairline,
        display: 'flex',
        alignItems: 'center',
      }}
    >
      <div css={{ flex: 1, display: 'flex' }}>
        <Link
          href={`/[projectId]/visualizer${idString}${queryString}`}
          as={`/${projectId}/visualizer${idString}${queryString}`}
          passHref
        >
          <Button variant={VARIANTS.ICON}>
            <BackSvg height={constants.icons.regular} />
          </Button>
        </Link>
      </div>

      <div>{filename}</div>

      <div css={{ flex: 1 }} />
    </div>
  )
}

AssetNavigation.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  query: PropTypes.string.isRequired,
  filename: PropTypes.string.isRequired,
}

export default AssetNavigation
