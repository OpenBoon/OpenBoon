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
        justifyContent: 'space-between',
        alignItems: 'center',
      }}
    >
      <Link
        href={`/[projectId]/visualizer${idString}${queryString}`}
        as={`/${projectId}/visualizer${idString}${queryString}`}
        passHref
      >
        <Button variant={VARIANTS.ICON}>
          <BackSvg height={constants.icons.regular} />
        </Button>
      </Link>

      <div>{filename}</div>

      <div css={{ width: spacing.base * 2 + constants.icons.regular }} />
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
