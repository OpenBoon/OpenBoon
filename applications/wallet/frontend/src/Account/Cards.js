import Link from 'next/link'
import PropTypes from 'prop-types'

import projectShape from '../Project/shape'

import { constants, colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import AccountUsagePlan from './UsagePlan'

const AccountCards = ({ projects }) => {
  return (
    <div css={{ display: 'flex', flexWrap: 'wrap' }}>
      {projects.map(({ id, name }) => (
        <div
          key={id}
          css={{
            paddingRight: spacing.spacious,
            paddingBottom: spacing.spacious,
            width: constants.form.maxWidth,
          }}
        >
          <div
            css={{
              display: 'flex',
              flexDirection: 'column',
              backgroundColor: colors.structure.smoke,
              boxShadow: constants.boxShadows.tableRow,
              borderRadius: constants.borderRadius.small,
            }}
          >
            <div css={{ padding: spacing.spacious }}>
              <h3 css={{ paddingBottom: spacing.base }}>Project: {name}</h3>
              <div
                css={{
                  color: colors.structure.zinc,
                  paddingBottom: spacing.normal,
                }}
              >
                Project ID: {id}
              </div>
              <div css={{ display: 'flex' }}>
                <Link href="/[projectId]" as={`/${id}`} passHref>
                  <Button variant={VARIANTS.PRIMARY_SMALL}>
                    View Dashboard
                  </Button>
                </Link>
              </div>
            </div>
            <AccountUsagePlan projectId={id} />
          </div>
        </div>
      ))}
    </div>
  )
}

AccountCards.propTypes = {
  projects: PropTypes.arrayOf(projectShape).isRequired,
}

export default AccountCards
