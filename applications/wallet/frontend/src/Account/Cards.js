import Link from 'next/link'
import PropTypes from 'prop-types'

import projectShape from '../Project/shape'

import { colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'
import Card from '../Card'

import AccountUsagePlan from './UsagePlan'

const AccountCards = ({ projects }) => {
  return (
    <div css={{ display: 'flex', flexWrap: 'wrap' }}>
      {projects.map(({ id, name }) => (
        <Card
          key={id}
          header={
            <div css={{ padding: spacing.normal }}>
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
          }
        >
          <AccountUsagePlan projectId={id} />
        </Card>
      ))}
    </div>
  )
}

AccountCards.propTypes = {
  projects: PropTypes.arrayOf(projectShape).isRequired,
}

export default AccountCards
