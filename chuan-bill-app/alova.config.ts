/* eslint-disable no-irregular-whitespace */

import type { Config } from '@alova/wormhole'

// For more config detailed visit:
// https://alova.js.org/tutorial/getting-started/extension-integration

export default <Config>{
  generator: [
    {
      /**
       * file input. support:
       * 1. openapi json file url
       * 2. local file
       */
      input: 'http://localhost:8080/v3/api-docs',
      /**
       * input file platform. Currently only swagger is supported.
       * When this parameter is specified, the input field only needs to specify the document address without specifying the openapi file
       */
      platform: 'swagger',

      /**
       * output path of interface file and type file.
       * Multiple generators cannot have the same address, otherwise the generated code will overwrite each other.
       */
      output: 'src/api',

      /**
       * the mediaType of the generated response data. default is `application/json`
       */
      responseMediaType: 'application/json',

      /**
       * the bodyMediaType of the generated request body data. default is `application/json`
       */
      bodyMediaType: 'application/json',

      /**
       * the generated api version. options are `2` or `3`, default is `auto`.
       */
      version: 3,

      /**
       * type of generated code. The options ​​are `auto/ts/typescript/module/commonjs`.
       */
      type: 'typescript',

      /**
       * exported global api name, you can access the generated api globally through this name, default is `Apis`.
       * it is required when multiple generators are configured, and it cannot be repeated
       */
      global: 'Apis',

      /**
       * filter or convert the generated api information, return an apiDescriptor, if this function is not specified, the apiDescripor object is not converted
       */
      handleApi: (apiDescriptor) => {
        // Skip logging to console
        // console.log(apiDescriptor)

        // Filter out any deprecated APIs if needed
        if (apiDescriptor.deprecated) {
          return undefined // Skip this API
        }
        // You can transform the API descriptor here if needed
        // For example, add custom headers, modify parameters, etc.
        const parameters = apiDescriptor.parameters
        const dtoParam = parameters?.find(item => item.name.endsWith('DTO'))
        if (dtoParam) {
          // eslint-disable-next-line ts/ban-ts-comment
          // @ts-ignore
          const properties = dtoParam.schema.properties
          const dtoParamters = Object.keys(properties).map((key) => {
            return {
              name: key,
              in: 'query',
              schema: {
                ...properties[key],
              },
            }
          })
          apiDescriptor.parameters = [...(apiDescriptor.parameters || []), ...dtoParamters]
        }
        return apiDescriptor
      },
    },
  ],

  /**
   * extension only
   * whether to automatically update the interface, enabled by default, check every 5 minutes, closed when set to `false`
   */
  autoUpdate: false,
}
