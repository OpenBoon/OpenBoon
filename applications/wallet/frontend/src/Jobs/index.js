import Head from 'next/head'
import { useRouter } from 'next/router'

import RefreshSvg from '../Icons/refresh.svg'

import { colors, spacing, typography } from '../Styles'

import PageTitle from '../PageTitle'
import Table from '../Table'
import Button, { VARIANTS } from '../Button'

import JobsEmpty from './Empty'
import JobsRow from './Row'

const REFRESH_HEIGHT = 32

const Jobs = () => {
  const {
    query: { projectId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Job Queue</title>
      </Head>

      <PageTitle>Job Queue</PageTitle>

      <div css={{ paddingTop: spacing.comfy, paddingBottom: spacing.normal }}>
        <Button
          variant={VARIANTS.PRIMARY}
          style={{
            height: REFRESH_HEIGHT,
          }}
          onClick={() => {}}>
          <div
            css={{
              display: 'flex',
              alignItems: 'center',
            }}>
            <RefreshSvg width={20} color={colors.structure.white} />
            <div css={{ paddingLeft: spacing.small }}>Refresh Jobs</div>
          </div>
        </Button>
      </div>

      <Table
        url={`/api/v1/projects/${projectId}/jobs/`}
        columns={[
          'Status',
          'Job Name',
          'Priority',
          'Created',
          '# Assets',
          'Errors',
          'Task Progress',
          '#Actions#',
        ]}
        expandColumn={2}
        renderEmpty={<JobsEmpty />}
        renderRow={({ result, revalidate }) => (
          <JobsRow
            key={result.id}
            projectId={projectId}
            job={result}
            revalidate={revalidate}
          />
        )}
      />
    </>
  )
}

export default Jobs
