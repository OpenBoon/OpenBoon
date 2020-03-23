import useSWR from 'swr'
import Link from 'next/link'

import { colors, spacing, typography } from '../Styles'

import Card from '../Card'
import Button, { VARIANTS } from '../Button'

import AccountDashboardUsagePlan from './UsagePlan'

const OverviewCards = () => {
  const {
    data: { results },
  } = useSWR(`/api/v1/projects`)

  return (
    <div css={{ display: 'flex', flexWrap: 'wrap' }}>
      {results.map(({ id, name }) => (
        <div
          key={id}
          css={{
            display: 'flex',
            flexDirection: 'column',
            flexWrap: 'wrap',
            maxHeight: '100vh',
            alignContent: 'flex-start',
          }}>
          <Card title="">
            <div
              css={{
                margin: -spacing.spacious,
              }}>
              <div
                css={{
                  padding: spacing.spacious,
                }}>
                <h3
                  css={{
                    fontSize: typography.size.medium,
                    lineHeight: typography.height.medium,
                  }}>
                  Project: {name}
                </h3>
                &nbsp;
                <div
                  css={{
                    fontSize: typography.size.regular,
                    lineHeight: typography.height.regular,
                    color: colors.structure.zinc,
                  }}>
                  Project ID: {id}
                </div>
                &nbsp;
                <div css={{ display: 'flex' }}>
                  <Link href="/[projectId]" as={`/${id}`} passHref>
                    <Button variant={VARIANTS.PRIMARY_SMALL}>
                      View Dashboard
                    </Button>
                  </Link>
                </div>
              </div>
              <AccountDashboardUsagePlan projectId={id} />
            </div>
          </Card>
        </div>
      ))}
    </div>
  )
}

export default OverviewCards
