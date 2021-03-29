import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, spacing, typography, constants } from '../Styles'

import Pagination from '../Pagination'

import TaskAssetsEmpty from './Empty'
import TaskAssetsRow from './Row'

const SIZE = 20

const TaskAssets = () => {
  const {
    query: { projectId, taskId, page = 1 },
  } = useRouter()

  const parsedPage = parseInt(page, 10)
  const from = parsedPage * SIZE - SIZE

  const {
    data: { count = 0, results },
  } = useSWR(
    `/api/v1/projects/${projectId}/tasks/${taskId}/assets/?from=${from}&size=${SIZE}`,
  )

  if (count === 0) {
    return <TaskAssetsEmpty />
  }

  return (
    <div css={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div
        css={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          paddingBottom: spacing.normal,
          flexShrink: 0,
        }}
      >
        <h3
          css={{
            color: colors.structure.zinc,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            fontWeight: typography.weight.regular,
          }}
        >
          Assets: {count}
        </h3>
      </div>

      <div css={{ boxShadow: constants.boxShadows.default }}>
        {results.map((asset, index) => (
          <TaskAssetsRow
            key={asset.id}
            projectId={projectId}
            index={index + 1 + from}
            asset={asset}
          />
        ))}
      </div>

      {count > 0 && <div>&nbsp;</div>}

      {count > 0 && (
        <Pagination
          currentPage={parsedPage}
          totalPages={Math.ceil(count / SIZE)}
        />
      )}

      {count > 0 && <div>&nbsp;</div>}
    </div>
  )
}

export default TaskAssets
