import Link from 'next/link'
import PropTypes from 'prop-types'

import { colors, spacing, constants, typography } from '../Styles'

const OrganizationsCard = ({ organization: { id, name, projectCount } }) => {
  return (
    <Link href={`/organizations/${id}`} passHref>
      <a
        css={{
          display: 'flex',
          flexDirection: 'column',
          backgroundColor: colors.structure.smoke,
          boxShadow: constants.boxShadows.tableRow,
          borderRadius: constants.borderRadius.medium,
          padding: spacing.spacious,
          border: constants.borders.regular.transparent,
          ':hover': {
            textDecoration: 'none',
            backgroundColor: colors.structure.iron,
            border: constants.borders.regular.steel,
          },
        }}
      >
        <h3
          title={name}
          css={{
            fontSize: typography.size.giant,
            lineHeight: typography.height.giant,
            fontWeight: typography.weight.medium,
            paddingBottom: spacing.base,
            overflow: 'hidden',
            whiteSpace: 'nowrap',
            textOverflow: 'ellipsis',
          }}
        >
          {name}
        </h3>
        <div
          css={{
            fontFamily: typography.family.condensed,
            color: colors.structure.zinc,
          }}
        >
          Projects: {projectCount}
        </div>
      </a>
    </Link>
  )
}

OrganizationsCard.propTypes = {
  organization: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    projectCount: PropTypes.number.isRequired,
  }).isRequired,
}

export default OrganizationsCard
