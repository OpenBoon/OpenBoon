import { useState } from 'react'
import Router, { useRouter } from 'next/router'

import { getQueryString } from '../Query/helpers'

const Search = () => {
  const {
    pathname,
    query: { q },
  } = useRouter()

  const [searchString, setSearchString] = useState(q || '')

  return (
    <div className="bg-gray-900 flex flex-1 justify-center">
      <div className="w-full xl:w-1/2 px-8">
        <form
          className="flex w-full"
          onSubmit={(event) => event.preventDefault()}
        >
          <input
            aria-label="Search assets by title or keyword, or drag image"
            placeholder="Search assets by title or keyword, or drag image"
            className="shadow appearance-none border rounded w-full py-2 px-3 mt-1 text-gray-700"
            type="search"
            value={searchString}
            onChange={({ target: { value } }) => {
              setSearchString(value)
            }}
          />

          <button
            className="px-3 hover:text-green-500"
            type="submit"
            onClick={() => {
              Router.push(`${pathname}${getQueryString({ q: searchString })}`)
            }}
          >
            <svg className="fill-current h-5 w-5" viewBox="0 0 20 20">
              <path d="M13.857 12.314h-.823l-.308-.308a6.438 6.438 0 001.645-4.32A6.672 6.672 0 007.686 1 6.672 6.672 0 001 7.686a6.672 6.672 0 006.686 6.685 6.438 6.438 0 004.32-1.645l.308.308v.823L17.457 19 19 17.457l-5.143-5.143zm-6.171 0a4.61 4.61 0 01-4.629-4.628 4.61 4.61 0 014.629-4.629 4.61 4.61 0 014.628 4.629 4.61 4.61 0 01-4.628 4.628z" />
            </svg>
          </button>
        </form>
      </div>
    </div>
  )
}

export default Search
