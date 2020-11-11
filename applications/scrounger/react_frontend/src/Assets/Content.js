import useSWR from 'swr'
import { useRouter } from 'next/router'

import { getQueryString } from '../Query/helpers'

const Assets = () => {
  const {
    query: { q },
  } = useRouter()

  const { data } = useSWR(
    `/api/v1/assets${getQueryString({
      from: 0,
      size: 40,
      text_search: q,
    })}`,
  )

  if (!data) return null

  const { assets } = data

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 p-1">
      {assets.map(({ id, name }) => {
        return (
          <div
            key={id}
            className="border-4 border-transparent hover:border-white"
          >
            <div
              className="relative bg-gray-900 flex justify-center items-center"
              style={{ paddingBottom: '100%' }}
            >
              <img
                className="absolute top-0 h-full w-full object-contain"
                src={`/api/v1/assets/${id}/thumbnail_file/`}
                alt={name}
              />
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default Assets
