import { useRouter } from 'next/router'
import useSWR from 'swr'
import useClipboard from 'react-use-clipboard'

import CopySvg from '../Icons/copy.svg'
import CheckmarkSvg from '../Icons/checkmark.svg'

import { spacing, constants, colors } from '../Styles'

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

  const [isCopied, setCopied] = useClipboard(JSON.stringify(results), {
    successDuration: 1000,
  })

  return (
    <button
      type="button"
      aria-label="Copy Search Query"
      title="Copy Search Query"
      onClick={setCopied}
      css={{
        paddingLeft: spacing.moderate,
        paddingRight: spacing.moderate,
        margin: 0,
        border: 'none',
        borderRadius: constants.borderRadius.small,
        backgroundColor: colors.structure.transparent,
        color: colors.structure.steel,
        ':hover, &.focus-visible:focus': {
          cursor: 'pointer',
          backgroundColor: colors.structure.mattGrey,
          color: colors.structure.white,
        },
      }}
    >
      {isCopied ? (
        <CheckmarkSvg height={constants.icons.regular} color={colors.key.one} />
      ) : (
        <CopySvg height={constants.icons.regular} />
      )}
    </button>
  )
}

export default FiltersCopyQuery
