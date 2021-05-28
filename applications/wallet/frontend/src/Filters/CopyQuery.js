import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import ButtonCopy from '../Button/Copy'

import { cleanup } from './helpers'

const FiltersCopyQuery = () => {
  const {
    query: { projectId, query },
  } = useRouter()

  const q = cleanup({ query })

  const { data: { results = '' } = {} } = useSWR(
    `/api/v1/projects/${projectId}/searches/raw_query/?query=${q}`,
    {
      suspense: false,
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  return (
    <div
      css={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        padding: spacing.small,
      }}
    >
      <ButtonCopy
        title="Search Query"
        value={JSON.stringify(results)}
        offset={100}
      />
    </div>
  )
}

export default FiltersCopyQuery
