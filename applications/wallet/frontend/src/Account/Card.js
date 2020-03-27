import useSWR from 'swr'
import Link from 'next/link'
import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'
import Card from '../Card'

import AccountUsagePlan from './UsagePlan'

const AccountCard = ({ id, name }) => {
  const { data: { results: subscriptions = [] } = {} } = useSWR(
    `/api/v1/projects/${id}/subscriptions/`,
  )

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
            Project ID: {id}
          </div>
          <div css={{ display: 'flex' }}>
            <Link href="/[projectId]" as={`/${id}`} passHref>
              <Button variant={VARIANTS.PRIMARY_SMALL}>View Dashboard</Button>
            </Link>
          </div>
        </div>
      }
      content={
        subscriptions.length !== 0 ? (
          <AccountUsagePlan subscriptions={subscriptions} />
        ) : (
          ''
        )
      }
    />
  )
}

AccountCard.propTypes = {
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
}

export default AccountCard
