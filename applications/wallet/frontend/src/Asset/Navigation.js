import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import BackSvg from '../Icons/back.svg'

const AssetNavigation = ({ projectId, assetId, query }) => {
  const idString = `?id=${assetId}`
  const queryString = query ? `&query=${query}` : ''

  return (
    <div
      css={{
        paddingLeft: spacing.base,
        paddingRight: spacing.base,
        backgroundColor: colors.structure.lead,
        color: colors.structure.steel,
        marginBottom: spacing.hairline,
        marginRight: spacing.hairline,
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
    </div>
  )
}

AssetNavigation.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  query: PropTypes.string.isRequired,
}

export default AssetNavigation
