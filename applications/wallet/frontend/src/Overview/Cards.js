import useSWR from 'swr'
import Link from 'next/link'

import { colors, spacing } from '../Styles'

import Card from '../Card'
import Button, { VARIANTS } from '../Button'

import AccountDashboardUsagePlan from './UsagePlan'

const OverviewCards = () => {
  const {
    data: { results, count },
  } = useSWR(`/api/v1/projects`)

  return (
    <>
      <h3 css={{ paddingTop: spacing.normal, paddingBottom: spacing.normal }}>
        Number of Projects: {count}
      </h3>
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
                      paddingBottom: spacing.base,
                    }}>
                    Project: {name}
                  </h3>
                  <div
                    css={{
                      color: colors.structure.zinc,
                      paddingBottom: spacing.normal,
                    }}>
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
                <AccountDashboardUsagePlan projectId={id} />
              </div>
            </Card>
          </div>
        ))}
      </div>
    </>
  )
}

export default OverviewCards
