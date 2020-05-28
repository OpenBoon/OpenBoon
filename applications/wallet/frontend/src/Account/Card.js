import useSWR from 'swr'
import Link from 'next/link'
import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'
import Card from '../Card'

import AccountUsagePlan from './UsagePlan'

const AccountCard = ({ projectId, name }) => {
  const {
    data: { results: subscriptions },
  } = useSWR(`/api/v1/projects/${projectId}/subscriptions/`)

  return (
    <Card
      header={
        <div css={{ padding: spacing.normal }}>
          <h3 css={{ paddingBottom: spacing.base }}>Project: {name}</h3>
          <div
            css={{
              color: colors.structure.zinc,
              paddingBottom: spacing.normal,
            }}
          >
            Project ID: {projectId}
          </div>
          <div css={{ display: 'flex' }}>
            <Link href="/[projectId]" as={`/${projectId}`} passHref>
              <Button variant={VARIANTS.PRIMARY_SMALL}>View Dashboard</Button>
            </Link>
          </div>
        </div>
      }
      content={
        // TODO: Update usage component to have the new plan info.
        // subscriptions.length !== 0 ? (
        //   <AccountUsagePlan subscriptions={subscriptions} />
        // ) : (
        //   ''
        // )
          ''
      }
    />
  )
}

AccountCard.propTypes = {
  projectId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
}

export default AccountCard
